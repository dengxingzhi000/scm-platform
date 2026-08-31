package com.scmcloud.document.template.service;

import com.scmcloud.document.template.domain.dto.TemplateVariable;
import lombok.extern.slf4j.Slf4j;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.ContentAccessor;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.Tc;
import org.docx4j.wml.Text;
import org.docx4j.wml.Tr;
import org.springframework.stereotype.Component;

import jakarta.xml.bind.JAXBElement;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于 Docx4j 的真实 DOCX 渲染引擎。
 * 目前支持 {{dot.path}} 文本变量替换(遍历段落与表格单元格的文本 runs)。
 * 条件渲染 / 循环 / 图片 / 二维码为后续扩展点。
 */
@Slf4j
@Component
public class Docx4jTemplateEngine implements TemplateEngine {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([\\w.]+)\\s*\\}\\}");

    @Override
    public byte[] render(byte[] templateBytes, Map<String, Object> data, List<TemplateVariable> variables) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(templateBytes)) {
            WordprocessingMLPackage pkg = WordprocessingMLPackage.load(in);
            replaceInContent(pkg.getMainDocumentPart().getContent(), data);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pkg.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Docx4j render failed", e);
        }
    }

    private void replaceInContent(List<Object> content, Map<String, Object> data) {
        if (content == null) {
            return;
        }
        for (Object obj : content) {
            Object value = (obj instanceof JAXBElement<?> je) ? je.getValue() : obj;
            if (value instanceof P p) {
                for (Object ro : p.getContent()) {
                    Object rv = (ro instanceof JAXBElement<?> re) ? re.getValue() : ro;
                    if (rv instanceof R r) {
                        replaceInRuns(r, data);
                    }
                }
            } else if (value instanceof Tbl tbl) {
                for (Object tro : tbl.getContent()) {
                    Object trv = (tro instanceof JAXBElement<?> te) ? te.getValue() : tro;
                    if (trv instanceof Tr tr) {
                        for (Object tco : tr.getContent()) {
                            Object tcv = (tco instanceof JAXBElement<?> tce) ? tce.getValue() : tco;
                            if (tcv instanceof Tc tc) {
                                for (Object p0 : tc.getContent()) {
                                    Object pv = (p0 instanceof JAXBElement<?> pe) ? pe.getValue() : p0;
                                    if (pv instanceof P p) {
                                        replaceInContent(p.getContent(), data);
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (value instanceof ContentAccessor ca) {
                replaceInContent(ca.getContent(), data);
            }
        }
    }

    private void replaceInRuns(R run, Map<String, Object> data) {
        for (Object ro : run.getContent()) {
            Object rv = (ro instanceof JAXBElement<?> re) ? re.getValue() : ro;
            if (rv instanceof Text text) {
                String txt = text.getValue();
                if (txt != null && txt.contains("{{")) {
                    text.setValue(replacePlaceholders(txt, data));
                }
            }
        }
    }

    private String replacePlaceholders(String text, Map<String, Object> data) {
        Matcher m = PLACEHOLDER.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            Object val = resolve(data, m.group(1));
            m.appendReplacement(sb, Matcher.quoteReplacement(val == null ? "" : String.valueOf(val)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private Object resolve(Map<String, Object> data, String path) {
        Object current = data;
        for (String seg : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = ((Map<?, ?>) map).get(seg);
        }
        return current;
    }
}
