import { z } from "zod";

export const loginSchema = z.object({
  username: z.string().min(3, "用户名至少3个字符"),
  password: z.string().min(6, "密码至少6个字符"),
});

export const userSchema = z.object({
  username: z.string().min(3).max(50),
  displayName: z.string().min(1).max(100),
  email: z.string().email().optional(),
  phone: z.string().regex(/^1[3-9]\d{9}$/, "手机号格式不正确").optional(),
  status: z.enum(["ACTIVE", "INACTIVE"]).default("ACTIVE"),
});

export const orderCreateSchema = z.object({
  userId: z.string().uuid(),
  skuId: z.string(),
  quantity: z.number().int().min(1, "数量至少为1"),
  remark: z.string().max(500).optional(),
});

export type LoginInput = z.infer<typeof loginSchema>;
export type UserInput = z.infer<typeof userSchema>;
export type OrderCreateInput = z.infer<typeof orderCreateSchema>;
