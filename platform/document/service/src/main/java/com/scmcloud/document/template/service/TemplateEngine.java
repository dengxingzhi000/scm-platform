package com.scmcloud.document.template.service;

import com.scmcloud.document.template.domain.dto.TemplateVariable;

import java.util.List;
import java.util.Map;

/**
 * 模板渲染引擎抽象。真实实现应在后续接入 Docx4j / Apache POI / 模板引擎,
 * 负责把模板字节 + 变量数据渲染为最终文档字节(支持 {{var}}、条件、循环、表格、二维码等)。
 */
public interface TemplateEngine {

    byte[] render(byte[] templateBytes, Map<String, Object> data, List<TemplateVariable> variables);
}
