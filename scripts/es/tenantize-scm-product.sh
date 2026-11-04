#!/usr/bin/env bash
# Tenant backfill for the shared `scm_product` Elasticsearch index.
#
# Why this exists
# ---------------
# `scm_product` is a single shared index. Brands appear only as `brandId` / `brandName`
# fields on the product document (there is no separate brand index), so brand-scoped
# queries inherit whatever isolation the product index has. The isolation is a mandatory
# `term tenant_id` filter in ProductSearchService.
#
# `tenant_id` is an additive keyword field, so `put_mapping` is used instead of a new
# index plus alias switch: Elasticsearch has no rename API, an alias cutover would copy
# the whole index, and it would not make rollback cheaper.
#
# Order of operations (do not skip ahead)
# ---------------------------------------
#   1. run this script (mapping + backfill + verification)
#   2. only after every document carries tenant_id, deploy the tenant-filtered code
#   3. the product sync consumer (Debezium -> Kafka) keeps writing tenant_id afterwards
#
# Rollback
# --------
#   * revert the deploy. The unused field stays in the mapping (harmless), or remove it
#     with the command printed at the end.
#
# Usage
# -----
#   ES_URL=http://localhost:9200 \
#   PGHOST=localhost PGPORT=5432 PGUSER=admin PGPASSWORD=admin123 \
#   ./scripts/es/tenantize-scm-product.sh
#
# Env: ES_URL, INDEX, PGHOST, PGPORT, PGUSER, PGPASSWORD, PRODUCT_DB, TENANT_DB, BATCH_SIZE
set -euo pipefail

ES_URL="${ES_URL:-http://localhost:9200}"
INDEX="${INDEX:-scm_product}"
PGHOST="${PGHOST:-localhost}"
PGPORT="${PGPORT:-5432}"
PGUSER="${PGUSER:-postgres}"
PRODUCT_DB="${PRODUCT_DB:-db_product}"
TENANT_DB="${TENANT_DB:-db_tenant}"
BATCH_SIZE="${BATCH_SIZE:-500}"

command -v jq >/dev/null || { echo "jq is required" >&2; exit 1; }
export PGPASSWORD="${PGPASSWORD:-}"

WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

es_get() { curl -fsS -H 'Content-Type: application/json' "$ES_URL/$1"; }

count_total="$(es_get "$INDEX/_count" | jq -r '.count')"
echo "before.total=$count_total"

echo "step 1/4: put_mapping tenant_id as keyword (additive, idempotent)"
curl -fsS -X PUT -H 'Content-Type: application/json' "$ES_URL/$INDEX/_mapping" -d '{
  "properties": {
    "tenant_id": { "type": "keyword" }
  }
}' >/dev/null

echo "step 2/4: export spu -> tenant from $PRODUCT_DB.prod_spu"
psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$TENANT_DB" -At \
  -c "SELECT id FROM tenant" > "$WORK_DIR/tenants.txt"

psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PRODUCT_DB" -At -F $'\t' \
  -c "SELECT id, tenant_id FROM prod_spu WHERE tenant_id IS NOT NULL" \
  > "$WORK_DIR/spu-tenants.raw.tsv"

source_rows="$(wc -l < "$WORK_DIR/spu-tenants.raw.tsv" | tr -d ' ')"
awk -F'\t' 'NR==FNR { valid[$1]=1; next } ($2 in valid) { print $1"\t"$2 }' \
  "$WORK_DIR/tenants.txt" "$WORK_DIR/spu-tenants.raw.tsv" > "$WORK_DIR/spu-tenants.valid.tsv"
invalid_rows=$(( source_rows - $(wc -l < "$WORK_DIR/spu-tenants.valid.tsv" | tr -d ' ') ))
echo "source.rows=$source_rows source.invalid_tenant=$invalid_rows"

# One tenant per spu id (the source table is unique on id, this is just defensive).
awk -F'\t' '{ map[$1]=$2 } END { for (p in map) print p"\t"map[p] }' \
  "$WORK_DIR/spu-tenants.valid.tsv" > "$WORK_DIR/spu-tenants.tsv"
echo "source.distinct_spus=$(wc -l < "$WORK_DIR/spu-tenants.tsv" | tr -d ' ')"

echo "step 3/4: update_by_query per tenant (batches of $BATCH_SIZE document ids)"
cut -f2 "$WORK_DIR/spu-tenants.tsv" | sort -u > "$WORK_DIR/tenants.used.txt"
while IFS= read -r tenant_id; do
  awk -F'\t' -v t="$tenant_id" '$2 == t { print $1 }' \
    "$WORK_DIR/spu-tenants.tsv" > "$WORK_DIR/ids-$tenant_id.txt"
  [ -s "$WORK_DIR/ids-$tenant_id.txt" ] || continue

  split -l "$BATCH_SIZE" -a 4 "$WORK_DIR/ids-$tenant_id.txt" "$WORK_DIR/batch-$tenant_id-"
  for batch_file in "$WORK_DIR"/batch-"$tenant_id"-*; do
    ids_json="$(awk '{ printf "\"%s\",", $0 }' "$batch_file" | sed 's/,$//')"
    body="$WORK_DIR/update-body.json"
    printf '{"query":{"ids":{"values":[%s]}}, ' "$ids_json" > "$body"
    printf '"script":{"source":"ctx._source.tenant_id = params.tenant","params":{"tenant":"%s"}}}' \
      "$tenant_id" >> "$body"

    curl -fsS -X POST -H 'Content-Type: application/json' \
      "$ES_URL/$INDEX/_update_by_query?conflicts=proceed" -d @"$body" \
      | jq -r --arg t "$tenant_id" '"tenant=\($t) updated=\(.updated) version_conflicts=\(.version_conflicts)"'
  done
done < "$WORK_DIR/tenants.used.txt"

curl -fsS -X POST "$ES_URL/$INDEX/_refresh" >/dev/null

echo "step 4/4: verification"
after_total="$(es_get "$INDEX/_count" | jq -r '.count')"
after_with_tenant="$(curl -fsS -X POST -H 'Content-Type: application/json' \
  "$ES_URL/$INDEX/_count" -d '{"query":{"exists":{"field":"tenant_id"}}}' | jq -r '.count')"
missing_tenant=$(( after_total - after_with_tenant ))
echo "after.total=$after_total after.with_tenant=$after_with_tenant after.missing_tenant=$missing_tenant"

if [ "$after_total" != "$count_total" ]; then
  echo "WARNING: document count changed during backfill ($count_total -> $after_total)" >&2
fi

echo "sample (3 spus, ES tenant_id vs DB tenant_id):"
shuf -n 3 "$WORK_DIR/spu-tenants.tsv" > "$WORK_DIR/sample.tsv" || head -n 3 "$WORK_DIR/spu-tenants.tsv" > "$WORK_DIR/sample.tsv"
while IFS=$'\t' read -r spu_id tenant_id; do
  es_tenant="$(es_get "$INDEX/_doc/$spu_id" | jq -r '._source.tenant_id // "missing"')"
  if [ "$es_tenant" = "$tenant_id" ]; then
    echo "  spu_id=$spu_id es=$es_tenant db=$tenant_id OK"
  else
    echo "  spu_id=$spu_id es=$es_tenant db=$tenant_id MISMATCH" >&2
    exit 2
  fi
done < "$WORK_DIR/sample.tsv"

if [ "$missing_tenant" -ne 0 ]; then
  cat >&2 <<MSG
$missing_tenant document(s) have no tenant_id. They have no $PRODUCT_DB.prod_spu row (or the
row carries no tenant) and are therefore invisible once the filtered code is deployed
(fail-closed). Register them or delete them before enabling enforcement.
MSG
  exit 3
fi

cat <<'MSG'
Backfill verified. Next steps:
  1. deploy the tenant-filtered search code
  2. verify with two tenant headers: brand/product queries must only return that tenant
Rollback (code revert, optional field cleanup):
  curl -X POST "$ES_URL/scm_product/_update_by_query?conflicts=proceed" \
    -H 'Content-Type: application/json' \
    -d '{"query":{"exists":{"field":"tenant_id"}},"script":{"source":"ctx._source.remove(\"tenant_id\")"}}'
MSG
