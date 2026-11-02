package com.scmcloud.file.service.ocr;

import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OcrServiceTest {

    @Test
    void recognizeTextShouldWrapUploadStreamFailure() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenThrow(new IOException("stream closed"));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> new OcrService().recognizeText(file));

        assertTrue(error.getMessage().contains("OCR识别失败"));
    }

    @Test
    void extractInvoiceShouldWrapUploadStreamFailure() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenThrow(new IOException("stream closed"));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> new OcrService().extractInvoiceInfo(file));

        assertTrue(error.getMessage().contains("OCR识别失败"));
    }

    @Test
    void extractContractShouldWrapUploadStreamFailure() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenThrow(new IOException("stream closed"));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> new OcrService().extractContractInfo(file));

        assertTrue(error.getMessage().contains("OCR识别失败"));
    }
}
