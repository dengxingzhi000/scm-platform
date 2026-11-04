#!/usr/bin/env bash
# Tenant backfill for the shared `products` Elasticsearch index.
#
# Why this exists
# ---------------
# `products` is one shared index; isolation is a mandatory `term tenant_id` filter in
# the search service. Adding the field is a mapping-only change (the field is additive,
# no existing type changes), so `put_mapping` is used instead of a new index plus alias
# switch: Elasticsearch has no rename API, so an alias cutover would require copying the
# whole index first, and it would not make rollback any cheaper.
#
# Order of operations (do not skip ahead)
# ---------------------------------------
#   1. run this script (mapping + backfill + verification)
#   2. only after every document carries `tenant_id`, deploy the filtered code
#   3. `POST /api/v1/search/index/reindex` is tenant-scoped after the deploy; it is not
#      needed for the backfill itself and must not be used to populate tenant_id
#
# Rollback
# --------
#   * revert the deploy. The unused `tenant_id` field stays in the mapping (harmless), or
#     clear it with the command printed at the end.
#
# Usage
# -----
#   ES_URL=http://localhost:9200 \
#   PGHOST=localhost PGPORT=5432 PGUSER=admin PGPASSWORD=admin123 \
#   ./scripts/es/tenantize-products.sh
#
# Env: ES_URL, INDEX, PGHOST, PGPORT, PGUSER, PGPASSWORD, MALL_DB, TENANT_DB, BATCH_SIZE
set -euo pipefail

ES_URL="${ES_URL:-http://localhost:9200}"
INDEX="${INDEX:-products}"
PGHOST="${PGHOST:-localhost}"
PGPORT="${PGPORT:-5432}"
PGUSER="${PGUSER:-postgres}"
MALL_DB="${MALL_DB:-db_mall}"
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

echo "step 2/4: export product -> tenant from $MALL_DB.mall_product_display"
psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$TENANT_DB" -At \
  -c "SELECT id FROM tenant" > "$WORK_DIR/tenants.txt"

psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$MALL_DB" -At -F $'\t' \
  -c "SELECT product_id, tenant_id FROM mall_product_display WHERE tenant_id IS NOT NULL" \
  > "$WORK_DIR/product-tenants.raw.tsv"

source_rows="$(wc -l < "$WORK_DIR/product-tenants.raw.tsv" | tr -d ' ')"
awk -F'\t' 'NR==FNR { valid[$1]=1; next } ($2 in valid) { print $1"\t"$2 }' \
  "$WORK_DIR/tenants.txt" "$WORK_DIR/product-tenants.raw.tsv" > "$WORK_DIR/product-tenants.valid.tsv"
invalid_rows=$(( source_rows - $(wc -l < "$WORK_DIR/product-tenants.valid.tsv" | tr -d ' ') ))
echo "source.rows=$source_rows source.invalid_tenant=$invalid_rows"

# One display row per product_id wins (the last one in the export).
awk -F'\t' '{ map[$1]=$2 } END { for (p in map) print p"\t"map[p] }' \
  "$WORK_DIR/product-tenants.valid.tsv" > "$WORK_DIR/product-tenants.tsv"
echo "source.distinct_products=$(wc -l < "$WORK_DIR/product-tenants.tsv" | tr -d ' ')"

echo "step 3/4: update_by_query per tenant (batches of $BATCH_SIZE product ids)"
cut -f2 "$WORK_DIR/product-tenants.tsv" | sort -u > "$WORK_DIR/tenants.used.txt"
while IFS= read -r tenant_id; do
  awk -F'\t' -v t="$tenant_id" '$2 == t { print $1 }' \
    "$WORK_DIR/product-tenants.tsv" > "$WORK_DIR/ids-$tenant_id.txt"
  [ -s "$WORK_DIR/ids-$tenant_id.txt" ] || continue

  split -l "$BATCH_SIZE" -a 4 "$WORK_DIR/ids-$tenant_id.txt" "$WORK_DIR/batch-$tenant_id-"
  for batch_file in "$WORK_DIR"/batch-"$tenant_id"-*; do
    ids_json="$(awk '{ printf "\"%s\",", $0 }' "$batch_file" | sed 's/,$//')"
    body="$WORK_DIR/update-body.json"
    printf '{"query":{"terms":{"product_id":[%s]}}, ' "$ids_json" > "$body"
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

echo "sample (3 products, ES tenant_id vs DB tenant_id):"
shuf -n 3 "$WORK_DIR/product-tenants.tsv" > "$WORK_DIR/sample.tsv" || head -n 3 "$WORK_DIR/product-tenants.tsv" > "$WORK_DIR/sample.tsv"
while IFS=$'\t' read -r product_id tenant_id; do
  es_tenant="$(curl -fsS -X POST -H 'Content-Type: application/json' "$ES_URL/$INDEX/_search" \
    -d "{\"size\":1,\"query\":{\"term\":{\"product_id\":\"$product_id\"}}}" \
    | jq -r '.hits.hits[0]._source.tenant_id // "missing"')"
  if [ "$es_tenant" = "$tenant_id" ]; then
    echo "  product_id=$product_id es=$es_tenant db=$tenant_id OK"
  else
    echo "  product_id=$product_id es=$es_tenant db=$tenant_id MISMATCH" >&2
    exit 2
  fi
done < "$WORK_DIR/sample.tsv"

if [ "$missing_tenant" -ne 0 ]; then
  cat >&2 <<MSG
$missing_tenant document(s) have no tenant_id. They have no row in $MALL_DB.mall_product_display
(or the row carries no tenant) and are therefore invisible once the filtered code is deployed
(fail-closed). Register them or delete them before enabling enforcement.
MSG
  exit 3
fi

cat <<'MSG'
Backfill verified. Next steps:
  1. deploy the tenant-filtered search code
  2. verify with a tenant header: GET /api/v1/search/products?keyword=... must only return that tenant
Rollback (code revert, optional field cleanup):
  curl -X POST "$ES_URL/products/_update_by_query?conflicts=proceed" \
    -H 'Content-Type: application/json' \
    -d '{"query":{"exists":{"field":"tenant_id"}},"script":{"source":"ctx._source.remove(\"tenant_id\")"}}'
MSG
