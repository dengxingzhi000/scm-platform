package com.scmcloud.file.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.file.service.ocr.OcrService;
import com.scmcloud.file.service.ocr.OcrService.ContractInfo;
import com.scmcloud.file.service.ocr.OcrService.InvoiceInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OcrControllerTest {

    @Mock
    private OcrService ocrService;

    @InjectMocks
    private OcrController controller;

    private MultipartFile file() {
        return new MockMultipartFile("file", "invoice.png", "image/png", "data".getBytes());
    }

    @Test
    void recognizeTextShouldDelegateTheUploadedFile() {
        MultipartFile file = file();
        when(ocrService.recognizeText(file)).thenReturn("text");

        ApiResponse<String> response = controller.recognizeText(file);

        assertEquals(200, response.code());
        assertEquals("text", response.data());
        verify(ocrService).recognizeText(file);
    }

    @Test
    void extractInvoiceShouldDelegateTheUploadedFile() {
        MultipartFile file = file();
        InvoiceInfo info = new InvoiceInfo();
        when(ocrService.extractInvoiceInfo(file)).thenReturn(info);

        assertSame(info, controller.extractInvoice(file).data());
    }

    @Test
    void extractContractShouldDelegateTheUploadedFile() {
        MultipartFile file = file();
        ContractInfo info = new ContractInfo();
        when(ocrService.extractContractInfo(file)).thenReturn(info);

        assertSame(info, controller.extractContract(file).data());
    }
}
