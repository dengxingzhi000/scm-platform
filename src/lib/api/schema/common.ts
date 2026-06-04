import { z } from "zod";

export const pageParamSchema = z.object({
  current: z.number().int().positive().default(1),
  size: z.number().int().positive().max(100).default(20),
});

export const idParamSchema = z.string().min(1, "ID不能为空");

export type PageParamInput = z.infer<typeof pageParamSchema>;
