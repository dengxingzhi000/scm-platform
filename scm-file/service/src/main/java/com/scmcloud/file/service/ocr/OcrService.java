package com.scmcloud.file.service.ocr;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

/**
 * OCR服务 - 提供图片文字识别功能。
 *
 * <p>支持的功能：
 * <ul>
 *   <li>通用文字识别</li>
 *   <li>发票信息提取</li>
 *   <li>合同关键信息提取</li>
 * </ul>
 */
@Slf4j
@Service
public class OcrService {

    @Value("${ocr.tessdata-path:./tessdata}")
    private String tessdataPath;

    @Value("${ocr.language:chi_sim+eng}")
    private String language;

    /**
     * 识别图片流中的文字。
     *
     * @param inputStream 图片输入流
     * @return 识别结果
     */
    public String recognizeText(InputStream inputStream) {
        try {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IOException("Unsupported or unreadable image");
            }
            ITesseract tesseract = createTesseract();
            String result = tesseract.doOCR(image);
            log.info("OCR识别完成: length={}", result.length());
            return result;
        } catch (IOException | TesseractException e) {
            throw new RuntimeException("OCR识别失败: " + e.getMessage(), e);
        }
    }

    /**
     * 提取发票信息。
     *
     * @param inputStream 发票图片流
     * @return 发票信息
     */
    public InvoiceInfo extractInvoiceInfo(InputStream inputStream) {
        String text = recognizeText(inputStream);
        return parseInvoiceText(text);
    }

    /**
     * 提取合同关键信息。
     *
     * @param inputStream 合同图片流
     * @return 合同信息
     */
    public ContractInfo extractContractInfo(InputStream inputStream) {
        String text = recognizeText(inputStream);
        return parseContractText(text);
    }

    private ITesseract createTesseract() {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessdataPath);
        tesseract.setLanguage(language);
        // 设置PSM模式为自动检测
        tesseract.setPageSegMode(3);
        return tesseract;
    }

    private InvoiceInfo parseInvoiceText(String text) {
        InvoiceInfo info = new InvoiceInfo();
        info.setRawText(text);

        // 提取发票号码
        Pattern invoiceNoPattern = Pattern.compile("发票号码[：:]?\\s*(\\d{8,20})");
        Matcher invoiceNoMatcher = invoiceNoPattern.matcher(text);
        if (invoiceNoMatcher.find()) {
            info.setInvoiceNo(invoiceNoMatcher.group(1));
        }

        // 提取开票日期
        Pattern datePattern = Pattern.compile("开票日期[：:]?\\s*(\\d{4}年\\d{1,2}月\\d{1,2}日|\\d{4}-\\d{1,2}-\\d{1,2})");
        Matcher dateMatcher = datePattern.matcher(text);
        if (dateMatcher.find()) {
            info.setInvoiceDate(dateMatcher.group(1));
        }

        // 提取金额
        Pattern amountPattern = Pattern.compile("金额[：:]?\\s*[¥￥]?([\\d,.]+)");
        Matcher amountMatcher = amountPattern.matcher(text);
        if (amountMatcher.find()) {
            info.setAmount(amountMatcher.group(1));
        }

        // 提取购买方名称
        Pattern buyerPattern = Pattern.compile("购买方[：:]?\\s*名称[：:]?\\s*([\\u4e00-\\u9fa5]+)");
        Matcher buyerMatcher = buyerPattern.matcher(text);
        if (buyerMatcher.find()) {
            info.setBuyerName(buyerMatcher.group(1));
        }

        // 提取销售方名称
        Pattern sellerPattern = Pattern.compile("销售方[：:]?\\s*名称[：:]?\\s*([\\u4e00-\\u9fa5]+)");
        Matcher sellerMatcher = sellerPattern.matcher(text);
        if (sellerMatcher.find()) {
            info.setSellerName(sellerMatcher.group(1));
        }

        return info;
    }

    private ContractInfo parseContractText(String text) {
        ContractInfo info = new ContractInfo();
        info.setRawText(text);

        // 提取合同编号
        Pattern contractNoPattern = Pattern.compile("合同编号[：:]?\\s*([A-Za-z0-9-]+)");
        Matcher contractNoMatcher = contractNoPattern.matcher(text);
        if (contractNoMatcher.find()) {
            info.setContractNo(contractNoMatcher.group(1));
        }

        // 提取甲方
        Pattern partyAPattern = Pattern.compile("甲方[：:]?\\s*([\\u4e00-\\u9fa5]+)");
        Matcher partyAMatcher = partyAPattern.matcher(text);
        if (partyAMatcher.find()) {
            info.setPartyA(partyAMatcher.group(1));
        }

        // 提取乙方
        Pattern partyBPattern = Pattern.compile("乙方[：:]?\\s*([\\u4e00-\\u9fa5]+)");
        Matcher partyBMatcher = partyBPattern.matcher(text);
        if (partyBMatcher.find()) {
            info.setPartyB(partyBMatcher.group(1));
        }

        // 提取合同金额
        Pattern amountPattern = Pattern.compile("合同金额[：:]?\\s*[¥￥]?([\\d,.]+)\\s*元?");
        Matcher amountMatcher = amountPattern.matcher(text);
        if (amountMatcher.find()) {
            info.setAmount(amountMatcher.group(1));
        }

        // 提取签订日期
        Pattern datePattern = Pattern.compile("签订日期[：:]?\\s*(\\d{4}年\\d{1,2}月\\d{1,2}日|\\d{4}-\\d{1,2}-\\d{1,2})");
        Matcher dateMatcher = datePattern.matcher(text);
        if (dateMatcher.find()) {
            info.setSignDate(dateMatcher.group(1));
        }

        return info;
    }

    /**
     * 发票信息。
     */
    @Data
    public static class InvoiceInfo {
        private String invoiceNo;
        private String invoiceDate;
        private String amount;
        private String buyerName;
        private String sellerName;
        private String rawText;
    }

    /**
     * 合同信息。
     */
    @Data
    public static class ContractInfo {
        private String contractNo;
        private String partyA;
        private String partyB;
        private String amount;
        private String signDate;
        private String rawText;
    }
}
