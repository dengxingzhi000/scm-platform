package com.scmcloud.document.template.domain.dto;

import lombok.Data;

/**
 * 模板变量定义。模板 Schema 由若干变量组成,
 * 渲染时按 name(支持 user.name 点路径)从入参取值。
 */
@Data
public class TemplateVariable {

    /** 变量名, 支持点路径, 如 user.name / contract.amount */
    private String name;

    /** 类型: String / Integer / Decimal / Boolean / Date / Image / QRCode / Barcode */
    private String type;

    /** 是否必填 */
    private boolean required;

    /** 默认值 */
    private String defaultValue;

    /** 说明 */
    private String description;

    /** 示例值 */
    private String example;
}
