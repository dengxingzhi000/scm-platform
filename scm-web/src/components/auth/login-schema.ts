import { z } from "zod";

/** next-intl 的翻译函数签名(只用到 key,不接参数)。 */
export type Translator = (key: string) => string;

/**
 * 校验 schema 是**工厂**而不是常量:错误文案要在 schema 构建时就翻译好,
 * 这样组件只要渲染 `errors.x?.message` 即可 ——
 * 校验错误是已翻译的文案,服务端错误(如验证码错误)是后端原样返回的文案,
 * 两条路径共用同一处渲染,不需要再做一层 key 映射。
 */
export function buildLoginSchema(t: Translator) {
  return z.object({
    username: z.string().min(1, t("errors.usernameRequired")),
    password: z.string().min(1, t("errors.passwordRequired")),
    // 「记住我」不提交给后端,但它属于表单值:放进 schema 才能让
    // useForm<LoginFormValues> 的 defaultValues / FormField name / Checkbox checked 三者类型对齐。
    // 用 optional 而不是 default(true):zod 的 default 会让输入类型(remember?)/输出类型(remember)
    // 分叉,而 zodResolver 与 useForm 要求两者一致;默认值由 defaultValues 提供。
    remember: z.boolean().optional(),
  });
}

export function buildTotpSchema(t: Translator) {
  return z.object({
    code: z
      .string()
      .min(1, t("errors.codeRequired"))
      .length(6, t("errors.codeLength"))
      .regex(/^\d+$/, t("errors.codeDigits")),
  });
}

export type LoginFormValues = z.infer<ReturnType<typeof buildLoginSchema>>;
export type TotpFormValues = z.infer<ReturnType<typeof buildTotpSchema>>;
