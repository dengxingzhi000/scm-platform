package com.cognora.platform.auth.service;

import com.cognora.platform.common.dto.user.TwoFactorSetupResponse;
import com.cognora.platform.common.dto.user.TwoFactorVerifyRequest;
import com.cognora.platform.common.security.util.TotpUtils;
import com.cognora.platform.common.web.domain.SecurityUser;
import com.cognora.platform.system.api.UserDubboService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TwoFactorServiceTest {

    private TotpUtils totpUtils;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private UserDubboService userDubboService;
    private TwoFactorService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        totpUtils = mock(TotpUtils.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        userDubboService = mock(UserDubboService.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new TwoFactorService(totpUtils, redisTemplate, userDubboService);
        authenticate(UUID.randomUUID(), "tester");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void setupStoresPendingSecretAndReturnsUri() {
        when(totpUtils.generateSecret()).thenReturn("SECRET");
        when(totpUtils.generateQrCodeUrl("tester", "Cognora Aura", "SECRET")).thenReturn("otpauth://uri");
        when(userDubboService.getUserByUsername("tester"))
                .thenReturn(SecurityUser.builder().username("tester").twoFactorEnabled(false).build());

        TwoFactorSetupResponse response = service.setup();

        assertEquals("SECRET", response.getSecret());
        assertEquals("otpauth://uri", response.getOtpauthUri());
        verify(valueOperations).set(anyString(), anyString(), any(java.time.Duration.class));
    }

    @Test
    void enablePersistsSecretAfterTotpVerification() {
        when(valueOperations.get(anyString())).thenReturn("SECRET");
        when(totpUtils.verifyCode("SECRET", "123456")).thenReturn(true);

        TwoFactorVerifyRequest request = new TwoFactorVerifyRequest();
        request.setCode("123456");
        service.enable(request);

        verify(userDubboService).enableTwoFactor(any(), anyString());
        verify(redisTemplate).delete(anyString());
    }

    @Test
    void enableRejectsWrongCode() {
        when(valueOperations.get(anyString())).thenReturn("SECRET");
        when(totpUtils.verifyCode("SECRET", "000000")).thenReturn(false);

        TwoFactorVerifyRequest request = new TwoFactorVerifyRequest();
        request.setCode("000000");

        assertThrows(com.cognora.platform.common.exception.BusinessException.class,
                () -> service.enable(request));
    }

    private void authenticate(UUID userId, String username) {
        SecurityUser principal = SecurityUser.builder().userId(userId).username(username).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
