import { z } from "zod";

import type { SupplierDTO, SupplierUpsertPayload } from "@/lib/api/contracts/supplier";

/**
 * 供应商主数据表单：Zod schema + 表单值 ↔ 请求体映射。
 *
 * 校验规则与后端 `SupSupplierServiceImpl.validateMasterData` 对齐
 * （编码 `^[A-Za-z0-9_-]{2,64}$`、名称必填 ≤256、税号 ≤64、
 * 信用等级 `A+/A/B+/B/C/D`、邮箱格式、注册资本 ≥ 0）。
 *
 * **PATCH 语义**（spec §4.2）：`PUT /suppliers/{id}` 不是全量替换 ——
 * 后端 `BeanUtils.copyProperties` 连 null 一起拷，MyBatis-Plus 默认
 * `FieldStrategy.NOT_NULL` 会跳过 null 字段，于是：
 *
 * - 键**不出现** → 该字段保持原值（「不改」）
 * - 键**是空串** → 真正写入空串（「清空」）
 *
 * `toSupplierPayload` 必须把这两种信号区分开，否则用户清空备注后
 * 「保存成功」但旧值还在。
 */

/** 后端 `SupSupplierServiceImpl.CREDIT_RATINGS` 允许的信用等级。 */
export const CREDIT_RATINGS = ["A+", "A", "B+", "B", "C", "D"] as const;

/** 后端 `validateMasterData` 允许的供应商类型。 */
const SUPPLIER_TYPE_VALUES: readonly number[] = [1, 2, 3, 4];

/**
 * 表单值类型 —— 全部字段都是字符串。
 *
 * 数字字段（注册资本 / 供应商类型 / 排序）在表单里保持字符串，
 * 提交时由 `toSupplierPayload` 转成数字：这样「留空」和「填 0」能区分开，
 * 受控输入也不必每次按键都做类型转换。
 */
export interface SupplierFormValues {
  supplierCode: string;
  supplierName: string;
  supplierNameEn: string;
  supplierType: string;
  businessType: string;
  legalPerson: string;
  registeredCapital: string;
  registrationNo: string;
  taxNo: string;
  establishmentDate: string;
  contactName: string;
  contactPhone: string;
  contactEmail: string;
  contactAddress: string;
  bankName: string;
  bankAccount: string;
  bankAccountName: string;
  creditRating: string;
  paymentTerms: string;
  paymentMethod: string;
  businessLicense: string;
  certificates: string;
  website: string;
  sortOrder: string;
  remark: string;
}

/**
 * 表单 schema。
 *
 * 第二个类型参数（输入类型）显式写成 `SupplierFormValues`，否则 Zod 会把它
 * 推成 `unknown`，`zodResolver` 就拿不到 `FieldValues` 约束，
 * `useForm<SupplierFormValues>({ resolver: zodResolver(supplierFormSchema) })`
 * 会编译不过。
 */
export const supplierFormSchema: z.ZodType<SupplierFormValues, SupplierFormValues> = z.object({
  // 编码留空时后端会自动生成，所以空串合法；非空则必须是 2-64 位字母数字下划线连字符。
  supplierCode: z
    .string()
    .trim()
    .refine(
      (value) => value === "" || /^[A-Za-z0-9_-]{2,64}$/.test(value),
      "供应商编码只能包含字母、数字、下划线或连字符，长度 2-64",
    ),
  supplierName: z
    .string()
    .trim()
    .min(1, "供应商名称不能为空")
    .max(256, "供应商名称长度不能超过 256"),
  supplierNameEn: z.string().max(256, "英文名称长度不能超过 256"),
  supplierType: z
    .string()
    .trim()
    .refine(
      (value) => value === "" || SUPPLIER_TYPE_VALUES.includes(Number(value)),
      "供应商类型仅支持 1-4",
    ),
  businessType: z.string().max(64, "业务类型长度不能超过 64"),
  legalPerson: z.string().max(128, "法人姓名长度不能超过 128"),
  registeredCapital: z
    .string()
    .trim()
    .refine(
      (value) => value === "" || (Number.isFinite(Number(value)) && Number(value) >= 0),
      "注册资本必须是非负数",
    ),
  registrationNo: z.string().max(64, "统一社会信用代码长度不能超过 64"),
  taxNo: z.string().max(64, "税号长度不能超过 64"),
  // 成立日期是 `<input type="date">` 的值（yyyy-MM-dd，10 个字符）；
  // 后端 `LocalDate` 收到空串会被 Jackson 读成 null，等于「不改」。
  establishmentDate: z.string().max(10, "成立日期长度不能超过 10"),
  contactName: z.string().max(128, "联系人长度不能超过 128"),
  contactPhone: z.string().max(32, "联系电话长度不能超过 32"),
  contactEmail: z.union([z.literal(""), z.string().email("联系邮箱格式不正确").max(128, "联系邮箱长度不能超过 128")], {
    error: "联系邮箱格式不正确",
  }),
  contactAddress: z.string().max(512, "联系地址长度不能超过 512"),
  bankName: z.string().max(256, "开户行长度不能超过 256"),
  bankAccount: z.string().max(64, "银行账号长度不能超过 64"),
  bankAccountName: z.string().max(128, "账户名称长度不能超过 128"),
  creditRating: z.union([z.literal(""), z.enum(CREDIT_RATINGS)], {
    error: "信用等级仅支持 A+/A/B+/B/C/D",
  }),
  paymentTerms: z.string().max(64, "付款条件长度不能超过 64"),
  paymentMethod: z.string().max(32, "付款方式长度不能超过 32"),
  businessLicense: z.string().max(512, "营业执照长度不能超过 512"),
  certificates: z.string(),
  website: z.string().max(256, "官网地址长度不能超过 256"),
  sortOrder: z
    .string()
    .trim()
    .refine((value) => value === "" || Number.isInteger(Number(value)), "排序必须是整数"),
  remark: z.string(),
});

/** 空表单的默认值 —— 每个字段都是空串。 */
export const supplierFormDefaults: SupplierFormValues = {
  supplierCode: "",
  supplierName: "",
  supplierNameEn: "",
  supplierType: "",
  businessType: "",
  legalPerson: "",
  registeredCapital: "",
  registrationNo: "",
  taxNo: "",
  establishmentDate: "",
  contactName: "",
  contactPhone: "",
  contactEmail: "",
  contactAddress: "",
  bankName: "",
  bankAccount: "",
  bankAccountName: "",
  creditRating: "",
  paymentTerms: "",
  paymentMethod: "",
  businessLicense: "",
  certificates: "",
  website: "",
  sortOrder: "",
  remark: "",
};

/**
 * DTO → 表单初值：每个字段转成字符串，缺失的转成空串。
 *
 * 只取表单托管的字段 —— `cooperationStartDate` 与全部状态 / 评分字段
 * 由服务端维护，不进表单（spec §5.4）。
 */
export function toSupplierFormValues(dto: SupplierDTO): SupplierFormValues {
  return {
    supplierCode: dto.supplierCode ?? "",
    supplierName: dto.supplierName ?? "",
    supplierNameEn: dto.supplierNameEn ?? "",
    supplierType: dto.supplierType != null ? String(dto.supplierType) : "",
    businessType: dto.businessType ?? "",
    legalPerson: dto.legalPerson ?? "",
    registeredCapital: dto.registeredCapital != null ? String(dto.registeredCapital) : "",
    registrationNo: dto.registrationNo ?? "",
    taxNo: dto.taxNo ?? "",
    establishmentDate: dto.establishmentDate ?? "",
    contactName: dto.contactName ?? "",
    contactPhone: dto.contactPhone ?? "",
    contactEmail: dto.contactEmail ?? "",
    contactAddress: dto.contactAddress ?? "",
    bankName: dto.bankName ?? "",
    bankAccount: dto.bankAccount ?? "",
    bankAccountName: dto.bankAccountName ?? "",
    creditRating: dto.creditRating ?? "",
    paymentTerms: dto.paymentTerms ?? "",
    paymentMethod: dto.paymentMethod ?? "",
    businessLicense: dto.businessLicense ?? "",
    certificates: dto.certificates ?? "",
    website: dto.website ?? "",
    sortOrder: dto.sortOrder != null ? String(dto.sortOrder) : "",
    remark: dto.remark ?? "",
  };
}

/**
 * 表单值 → 请求体。
 *
 * 键的取舍就是 PATCH 语义：
 *
 * - 文本字段**始终出现**，空串保留为 `""` —— 空串是「清空」的信号。
 * - 数字字段留空时**不出现** —— 空串在数字上没有意义，「不出现」才是「不改」。
 * - `cooperationStartDate` 与 `supplierLevel` / `auditStatus` / `riskLevel` /
 *   `blacklisted` / `overallScore` 永远不出现 —— 服务端托管。
 */
export function toSupplierPayload(values: SupplierFormValues): SupplierUpsertPayload {
  const payload: SupplierUpsertPayload = {
    supplierCode: values.supplierCode,
    supplierName: values.supplierName,
    supplierNameEn: values.supplierNameEn,
    businessType: values.businessType,
    legalPerson: values.legalPerson,
    registrationNo: values.registrationNo,
    taxNo: values.taxNo,
    establishmentDate: values.establishmentDate,
    contactName: values.contactName,
    contactPhone: values.contactPhone,
    contactEmail: values.contactEmail,
    contactAddress: values.contactAddress,
    bankName: values.bankName,
    bankAccount: values.bankAccount,
    bankAccountName: values.bankAccountName,
    paymentTerms: values.paymentTerms,
    paymentMethod: values.paymentMethod,
    businessLicense: values.businessLicense,
    certificates: values.certificates,
    website: values.website,
    remark: values.remark,
  };

  // 数字字段：空串 → 键不出现（「不改」）；非空串 → 数字。
  if (values.supplierType !== "") {
    payload.supplierType = Number(values.supplierType);
  }
  if (values.registeredCapital !== "") {
    payload.registeredCapital = Number(values.registeredCapital);
  }
  if (values.sortOrder !== "") {
    payload.sortOrder = Number(values.sortOrder);
  }

  // 信用等级空串同样不出现：`sup_supplier.credit_rating` 上有
  // `CHECK (credit_rating IS NULL OR credit_rating IN ('A+','A','B+','B','C','D'))`，
  // 写空串会撞约束（500）。这个接口也清不掉信用等级 —— null 会被
  // MyBatis-Plus 的 NOT_NULL 策略跳过，所以「不出现」是唯一安全的选择。
  if (values.creditRating !== "") {
    payload.creditRating = values.creditRating;
  }

  return payload;
}
