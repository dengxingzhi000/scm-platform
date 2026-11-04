#!/usr/bin/env node
/**
 * Report-only scanner for GBK/UTF-8 double-encoding artifacts.
 *
 * This file is intentionally ASCII-only: the patterns are written as code-point
 * arrays so that the scanner does not flag itself and survives any encoding
 * round-trip on the build machine.
 *
 * Usage:
 *   node scripts/quality/check-mojibake.mjs [--root=<dir>] [--max=<n>] [--strict]
 *
 * Default behaviour is report-only and always exits 0. `--strict` exits 1 when
 * hits are found, so the same script can be promoted to a blocking CI gate later.
 */

import { readdirSync, readFileSync, statSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = resolve(HERE, '..', '..');

const args = process.argv.slice(2);
const rootArg = args.find((a) => a.startsWith('--root='));
const maxArg = args.find((a) => a.startsWith('--max='));
const strict = args.includes('--strict');
const ROOT = rootArg ? resolve(rootArg.slice('--root='.length)) : REPO_ROOT;
const MAX_HITS = maxArg ? Number(maxArg.slice('--max='.length)) : 300;

const TEXT_EXTENSIONS = new Set([
  '.java', '.kt', '.ts', '.tsx', '.js', '.mjs', '.cjs', '.yml', '.yaml',
  '.sql', '.md', '.txt', '.xml', '.properties', '.json', '.sh', '.bat',
  '.ps1', '.css', '.scss', '.html', '.vue'
]);

const SKIP_DIRS = new Set([
  '.git', '.next', '.idea', '.vscode', '.trae', 'node_modules', 'target',
  'build', 'dist', 'coverage', 'out', '.gradle', 'vendor'
]);

const SKIP_FILES = new Set([
  'package-lock.json',
  // catalogues known-corrupted lines; scanning it would always dominate the report
  'mojibake-report.md'
]);

const MAX_FILE_BYTES = 2 * 1024 * 1024;

/** Wildcard slot: any single code point is accepted (e.g. U+FF04 vs U+0024). */
const ANY = -1;

/**
 * Fragments produced when UTF-8 Chinese was decoded as GBK.
 * Code points were taken from this repository's own corrupted files.
 */
const PATTERNS = [
  { name: 'query', cps: [0x93cc, 0x30e8, 0xe1d7] },
  { name: 'role', cps: [0x7459, 0x6395, 0x58ca] },
  { name: 'delete', cps: [0x9352, 0x72bb, 0x6ace] },
  { name: 'permission', cps: [0x93c9, 0x51ae, 0x6aba] },
  { name: 'grant', cps: [0x93ba, 0x581f, 0x6f48] },
  { name: 'modify', cps: [0x6dc7, 0xe1bd, 0x657c] },
  { name: 'add', cps: [0x93c2, 0x677f, 0xe583] },
  { name: 'data', cps: [0x93c1, 0x7248, 0x5d41] },
  { name: 'encrypt', cps: [0x9354, 0x72b2, 0x7611] },
  { name: 'failure', cps: [0x6fb6, 0x8fab, 0x89e6] },
  { name: 'reconciliation', cps: [0x7035, 0x7845, 0x5904] },
  { name: 'record', cps: [0x7481, 0x677f, 0x7d8d] },
  { name: 'create', cps: [0x9352, 0x6d98, 0x7f13] },
  { name: 'control', cps: [0x93ba, 0x0443, 0x57d7] },
  { name: 'manage', cps: [0x7ee0, ANY, 0x608a] },
  { name: 'based-on', cps: [0x93cd, 0x89c4, 0x5d41] },
  { name: 'user', cps: [0x9422, 0x3126, 0x57db] },
  { name: 'method', cps: [0x93c2, 0x89c4, 0x7876] },
  { name: 'used-for', cps: [0x9422, 0x3124, 0x7c2c] },
  { name: 'load', cps: [0x9354, 0x72ba, 0x6d47] },
  { name: 'mapping', cps: [0x93c4, 0x72b2, 0x76a0] },
  { name: 'gbk-residue', cps: [0x951f] }
];

/** Legitimate Chinese place names that must never be reported. */
const WHITELIST = [
  [0x6dc7, 0x53bf],
  [0x6dc7, 0x6c34]
];

function isPrivateUse(cp) {
  return cp >= 0xe000 && cp <= 0xf8ff;
}

function isSkippedFile(relPath) {
  const base = relPath.split(/[/\\]/).pop();
  return SKIP_FILES.has(base);
}

function collectFiles(dir, acc) {
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const full = join(dir, entry.name);
    if (entry.isDirectory()) {
      if (SKIP_DIRS.has(entry.name)) continue;
      collectFiles(full, acc);
      continue;
    }
    if (!entry.isFile()) continue;
    const dot = entry.name.lastIndexOf('.');
    if (dot < 0) continue;
    const ext = entry.name.slice(dot).toLowerCase();
    if (!TEXT_EXTENSIONS.has(ext)) continue;
    if (isSkippedFile(relative(ROOT, full))) continue;
    try {
      if (statSync(full).size > MAX_FILE_BYTES) continue;
    } catch {
      continue;
    }
    acc.push(full);
  }
}

function findWhitelisted(cps) {
  const mask = new Array(cps.length).fill(false);
  for (const word of WHITELIST) {
    for (let i = 0; i + word.length <= cps.length; i++) {
      let hit = true;
      for (let k = 0; k < word.length; k++) {
        if (cps[i + k] !== word[k]) { hit = false; break; }
      }
      if (hit) {
        for (let k = 0; k < word.length; k++) mask[i + k] = true;
      }
    }
  }
  return mask;
}

function matchAt(cps, i, pattern) {
  if (i + pattern.length > cps.length) return false;
  for (let k = 0; k < pattern.length; k++) {
    const expected = pattern[k];
    if (expected !== ANY && cps[i + k] !== expected) return false;
  }
  return true;
}

function scanLine(cps, mask) {
  for (let i = 0; i < cps.length; i++) {
    if (mask[i]) continue;
    if (isPrivateUse(cps[i])) return 'private-use';
    for (const pattern of PATTERNS) {
      if (matchAt(cps, i, pattern.cps)) return pattern.name;
    }
  }
  return null;
}

function main() {
  const files = [];
  collectFiles(ROOT, files);

  let scanned = 0;
  let hitLines = 0;
  const hitFiles = new Set();
  const lines = [];

  for (const file of files) {
    let text;
    try {
      text = readFileSync(file, 'utf8');
    } catch {
      continue;
    }
    scanned++;
    const rel = relative(ROOT, file).split('\\').join('/');
    const rows = text.split(/\r?\n/);
    for (let n = 0; n < rows.length; n++) {
      const cps = Array.from(rows[n], (ch) => ch.codePointAt(0));
      const mask = findWhitelisted(cps);
      const reason = scanLine(cps, mask);
      if (!reason) continue;
      hitLines++;
      hitFiles.add(rel);
      if (lines.length < MAX_HITS) {
        const snippet = rows[n].trim().slice(0, 120);
        lines.push(`${rel}:${n + 1}  [${reason}]  ${snippet}`);
      }
    }
  }

  console.log(`Mojibake scan (report only) — root: ${ROOT}`);
  console.log(`Scanned ${scanned} text files, found ${hitLines} hit line(s) in ${hitFiles.size} file(s).`);
  if (lines.length) {
    console.log('');
    for (const line of lines) console.log(line);
    if (hitLines > lines.length) {
      console.log(`... ${hitLines - lines.length} more hit line(s) not shown (raise --max to see them)`);
    }
  }

  if (strict && hitLines > 0) {
    console.log('\n--strict: failing because mojibake artifacts are present.');
    process.exitCode = 1;
  }
}

main();
