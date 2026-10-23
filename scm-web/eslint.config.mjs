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
    files: ["src/**/*.{ts,tsx}"],
    rules: {
      // Enforce the bundle-size rules from the frontend stack decision.
      // Charts and heavyweight grids may only be imported from their lazy
      // wrappers (dynamic + ssr:false); ECharts must go through the
      // tree-shaken `echarts/core` registration.
      "no-restricted-imports": [
        "error",
        {
          paths: [
            {
              name: "echarts",
              message:
                "Import '@/lib/charts/echarts-core' (tree-shaken) via '@/components/charts/echart' instead of the full ECharts bundle.",
            },
            {
              name: "echarts-for-react",
              message:
                "Use '@/components/charts/echart' (dynamic + ssr:false) instead of echarts-for-react.",
            },
            {
              name: "ag-grid-community",
              message:
                "Use '@/components/data-grid/data-grid'; AG Grid must load client-side only.",
            },
            {
              name: "ag-grid-react",
              message:
                "Use '@/components/data-grid/data-grid'; AG Grid must load client-side only.",
            },
            {
              name: "@antv/g6",
              message:
                "Use '@/components/charts/topology-graph'; G6 must load client-side only.",
            },
          ],
        },
      ],
    },
  },
  {
    files: [
      "src/lib/charts/**/*.{ts,tsx}",
      "src/components/charts/**/*.{ts,tsx}",
      "src/components/data-grid/**/*.{ts,tsx}",
    ],
    rules: {
      "no-restricted-imports": "off",
    },
  },
  {
    ignores: [
      ".next/**",
      "out/**",
      "build/**",
      "next-env.d.ts",
      "coverage/**",
    ],
  },
];

export default eslintConfig;
