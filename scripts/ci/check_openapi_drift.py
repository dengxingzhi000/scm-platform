#!/usr/bin/env python3
"""OpenAPI 快照与 Spring MVC handler 双向静态校验。

用法：python3 scripts/ci/check_openapi_drift.py
退出码：0 一致；1 存在漂移（快照缺 handler 或 handler 未入快照）。

校验范围（按服务逐项比较 controller 的 @RequestMapping/@GetMapping/... 映射
与对应快照里的路径 + HTTP 方法）：
  * lms-tms       -> docs/api/openapi-scm-logistics.json
  * scm-inventory -> docs/api/openapi-scm-inventory.json

仅使用标准库，CI 直接可跑。
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

TARGETS = (
    (
        "lms-tms",
        ROOT / "lms/tms/service/src/main/java/com/cognora/lms/logistics/controller",
        ROOT / "docs/api/openapi-scm-logistics.json",
    ),
    (
        "scm-inventory",
        ROOT / "platform/inventory/service/src/main/java/com/cognora/platform/inventory/controller",
        ROOT / "docs/api/openapi-scm-inventory.json",
    ),
)

HTTP_METHODS = ("get", "post", "put", "delete", "patch", "head", "options")

CLASS_BASE_RE = re.compile(r'@RequestMapping\(\s*(?:value\s*=\s*)?"([^"]+)"')
MAPPING_RE = re.compile(
    r'@(Get|Post|Put|Delete|Patch)Mapping(?:\(\s*(?:value\s*=\s*)?"([^"]*)"[^)]*\))?')
REQUEST_MAPPING_METHOD_RE = re.compile(
    r'@RequestMapping\(\s*(?:value\s*=\s*)?"([^"]*)"[^)]*method\s*=\s*RequestMethod\.(GET|POST|PUT|DELETE|PATCH)')


def normalize(path: str) -> str:
    path = "/" + path.strip("/") if path.strip("/") else "/"
    return path


def join_paths(base: str, sub: str) -> str:
    if not sub:
        return normalize(base)
    return normalize(base.rstrip("/") + "/" + sub.lstrip("/"))


def scan_code_mappings(controller_dir: Path):
    mappings = set()
    if not controller_dir.is_dir():
        return mappings
    for java_file in sorted(controller_dir.glob("*.java")):
        text = java_file.read_text(encoding="utf-8")
        base_match = CLASS_BASE_RE.search(text)
        base = base_match.group(1) if base_match else ""
        for match in MAPPING_RE.finditer(text):
            method = match.group(1).upper()
            sub = match.group(2) or ""
            mappings.add((method, join_paths(base, sub)))
        for match in REQUEST_MAPPING_METHOD_RE.finditer(text):
            request_path = match.group(1)
            request_method = match.group(2)
            mappings.add((request_method, join_paths(base, request_path)))
    return mappings


def scan_snapshot_mappings(snapshot: Path):
    data = json.loads(snapshot.read_text(encoding="utf-8"))
    mappings = set()
    for path, item in data.get("paths", {}).items():
        for method in item:
            if method.lower() in HTTP_METHODS:
                mappings.add((method.upper(), normalize(path)))
    return mappings


def check_target(name: str, controller_dir: Path, snapshot: Path) -> bool:
    code = scan_code_mappings(controller_dir)
    snapshot_mappings = scan_snapshot_mappings(snapshot)

    added = sorted(code - snapshot_mappings)
    missing = sorted(snapshot_mappings - code)

    print(f"[{name}] snapshot={len(snapshot_mappings)} mappings, code={len(code)} mappings")
    print(f"  added (code has, snapshot missing): {len(added)}")
    for method, path in added:
        print(f"    + {method} {path}")
    print(f"  missing (snapshot has, code missing): {len(missing)}")
    for method, path in missing:
        print(f"    - {method} {path}")

    return not added and not missing


def main() -> int:
    failed_targets = []
    for name, controller_dir, snapshot in TARGETS:
        if not check_target(name, controller_dir, snapshot):
            failed_targets.append(name)

    if failed_targets:
        print(f"RESULT: FAILED - snapshot and controller mappings drifted: {', '.join(failed_targets)}")
        return 1
    print("RESULT: PASSED")
    return 0


if __name__ == "__main__":
    sys.exit(main())
