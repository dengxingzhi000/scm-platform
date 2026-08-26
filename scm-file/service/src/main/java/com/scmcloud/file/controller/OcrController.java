package com.scmcloud.file.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.file.service.ocr.OcrService;
import com.scmcloud.file.service.ocr.OcrService.ContractInfo;
import com.scmcloud.file.service.ocr.OcrService.InvoiceInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * OCR控制器 - 提供图片文字识别API。
 */
@Slf4j
@RestController
@RequestMapping("/file/ocr")
@RequiredArgsConstructor
public class OcrController {
    private final OcrService ocrService;

    /**
     * 识别图片中的文字。
     *
     * @param file 图片文件
     * @return 识别结果
     */
    @PostMapping("/recognize")
    public ApiResponse<String> recognizeText(@RequestParam("file") MultipartFile file) throws IOException {
        String result = ocrService.recognizeText(file.getInputStream(), file.getOriginalFilename());
        return ApiResponse.success(result);
    }

    /**
     * 提取发票信息。
     *
     * @param file 发票图片
     * @return 发票信息
     */
    @PostMapping("/invoice")
    public ApiResponse<InvoiceInfo> extractInvoice(@RequestParam("file") MultipartFile file) throws IOException {
        // 保存到临时文件
        File tempFile = File.createTempFile("invoice-", "-" + file.getOriginalFilename());
        file.transferTo(tempFile);
        try {
            InvoiceInfo info = ocrService.extractInvoiceInfo(tempFile);
            return ApiResponse.success(info);
        } finally {
            deleteTempFile(tempFile);
        }
    }

    /**
     * 提取合同关键信息。
     *
     * @param file 合同图片
     * @return 合同信息
     */
    @PostMapping("/contract")
    public ApiResponse<ContractInfo> extractContract(@RequestParam("file") MultipartFile file) throws IOException {
        File tempFile = File.createTempFile("contract-", "-" + file.getOriginalFilename());
        file.transferTo(tempFile);
        try {
            ContractInfo info = ocrService.extractContractInfo(tempFile);
            return ApiResponse.success(info);
        } finally {
            deleteTempFile(tempFile);
        }
    }

    private void deleteTempFile(File tempFile) {
        try {
            Files.deleteIfExists(tempFile.toPath());
        } catch (IOException e) {
            log.warn("临时文件清理失败: {}", tempFile.getAbsolutePath(), e);
        }
    }
}
