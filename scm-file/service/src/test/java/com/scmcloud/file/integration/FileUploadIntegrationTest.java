package com.scmcloud.file.integration;

import com.scmcloud.file.ScmFileApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ScmFileApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FileUploadIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldUploadFile() {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource("test content".getBytes()) {
            @Override
            public String getFilename() {
                return "test.txt";
            }
        });
        body.add("bizType", "product");
        body.add("bizId", "123");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/files/upload", request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void shouldCheckFileExistByMd5() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/files/check/{md5}", String.class, "nonexistent");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
