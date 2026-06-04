import { defineConfig } from "orval";

export default defineConfig({
  scm: {
    input: {
      target: "http://localhost:8761/v3/api-docs",
    },
    output: {
      target: "./src/lib/api/generated/api.ts",
      schemas: "./src/lib/api/generated/schemas",
      client: "react-query",
      mock: false,
      prettier: true,
      clean: true,
      override: {
        mutator: {
          path: "./src/lib/api/custom/instance.ts",
          name: "customInstance",
        },
        query: {
          useQuery: true,
          useMutation: true,
          options: {
            staleTime: 10000,
          },
        },
        operations: {
          login: {
            query: {
              useMutation: true,
            },
          },
        },
      },
    },
    hooks: {
      afterAllFilesWrite: "prettier --write",
    },
  },
});
