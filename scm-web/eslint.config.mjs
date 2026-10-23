import nextVitals from "eslint-config-next/core-web-vitals";

/**
 * Flat ESLint config.
 *
 * ESLint 10 removed the legacy `.eslintrc` format, so the previous
 * `.eslintrc.json` (`{"extends": ["next/core-web-vitals"]}`) is replaced by
 * this file. `eslint-config-next@16` ships native flat-config arrays:
 *   - `core-web-vitals` => base rules + `plugin:@next/next/core-web-vitals`
 *   - `typescript`      => `typescript-eslint` recommended rules
 *
 * `eslint-config-next/typescript` is intentionally not enabled: the old
 * eslintrc setup never enabled the typescript-eslint rules, so enabling them
 * here would introduce a new rule surface (and new failures) unrelated to the
 * dependency upgrade.
 */
const eslintConfig = [
  ...nextVitals,
  {
    rules: {
      // New React Compiler lint rules introduced by eslint-plugin-react-hooks v7
      // (pulled in by eslint-config-next@16). They flag pre-existing state-sync
      // patterns in this codebase, which need a dedicated refactor; staged as
      // warnings so the upgrade does not block on it.
      "react-hooks/set-state-in-effect": "warn",
    },
  },
  {
    ignores: [".next/**", "out/**", "build/**", "next-env.d.ts"],
  },
];

export default eslintConfig;
