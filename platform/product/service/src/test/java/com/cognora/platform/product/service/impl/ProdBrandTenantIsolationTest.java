package com.cognora.platform.product.service.impl;

import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.common.web.domain.SecurityUser;
import com.cognora.platform.product.domain.entity.ProdBrand;
import com.cognora.platform.product.mapper.ProdBrandMapper;
import com.cognora.platform.product.support.ProductTenantResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 品牌读写按登录态租户隔离；请求体里的 tenantId 不能改写归属。
 */
class ProdBrandTenantIsolationTest {

    private static final UUID TENANT_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID TENANT_B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private ProdBrandMapper mapper;
    private ProdBrandServiceImpl service;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, ProdBrand.class);

        mapper = mock(ProdBrandMapper.class);
        service = new ProdBrandServiceImpl(new ProductTenantResolver());
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    private void authenticate(UUID tokenTenant, UUID headerTenant) {
        SecurityUser user = SecurityUser.builder().userId(UUID.randomUUID()).username("tester")
                .tenantId(tokenTenant).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        if (headerTenant != null) {
            TenantContextHolder.setTenantId(headerTenant);
        }
    }

    @Test
    void createStampsTheAuthenticatedTenantAndIgnoresMatchingBodyTenant() {
        authenticate(TENANT_A, TENANT_A);

        ProdBrand brand = new ProdBrand();
        brand.setTenantId(TENANT_A.toString());

        ProdBrand created = service.createBrand(brand);

        assertEquals(TENANT_A.toString(), created.getTenantId());
        verify(mapper).insert(brand);
    }

    @Test
    void createRejectsBodyTenantFromAnotherTenant() {
        authenticate(TENANT_A, TENANT_A);

        ProdBrand brand = new ProdBrand();
        brand.setTenantId(TENANT_B.toString());

        assertThrows(AccessDeniedException.class, () -> service.createBrand(brand));
        verify(mapper, never()).insert(any(ProdBrand.class));
    }

    @Test
    void createRejectsWhenTokenTenantDiffersFromHeaderTenant() {
        authenticate(TENANT_A, TENANT_B);

        assertThrows(AccessDeniedException.class, () -> service.createBrand(new ProdBrand()));
        verify(mapper, never()).insert(any(ProdBrand.class));
    }

    @Test
    void createRejectsWhenNoTenantContextExists() {
        SecurityContextHolder.clearContext();

        assertThrows(AccessDeniedException.class, () -> service.createBrand(new ProdBrand()));
        verify(mapper, never()).insert(any(ProdBrand.class));
    }

    @Test
    void updateRejectsBodyTenantFromAnotherTenant() {
        authenticate(TENANT_A, TENANT_A);

        ProdBrand brand = new ProdBrand();
        brand.setTenantId(TENANT_B.toString());

        assertThrows(AccessDeniedException.class, () -> service.updateBrand("brand-1", brand));
        verify(mapper, never()).updateById(any(ProdBrand.class));
    }

    @Test
    void updateClearsTenantSoOwnershipCannotBeMoved() {
        authenticate(TENANT_A, TENANT_A);
        when(mapper.selectById("brand-1")).thenReturn(ownedBrand(TENANT_A));

        ProdBrand brand = new ProdBrand();
        brand.setBrandName("renamed");
        service.updateBrand("brand-1", brand);

        assertNull(brand.getTenantId());
        verify(mapper).updateById(brand);
    }

    @Test
    void updateOfAnotherTenantsBrandIsForbidden() {
        authenticate(TENANT_A, TENANT_A);
        when(mapper.selectById("brand-b")).thenReturn(ownedBrand(TENANT_B));

        assertThrows(AccessDeniedException.class, () -> service.updateBrand("brand-b", new ProdBrand()));
        verify(mapper, never()).updateById(any(ProdBrand.class));
    }

    @Test
    void deleteOfAnotherTenantsBrandIsForbidden() {
        authenticate(TENANT_A, TENANT_A);
        when(mapper.selectById("brand-b")).thenReturn(ownedBrand(TENANT_B));

        assertThrows(AccessDeniedException.class, () -> service.deleteBrand("brand-b"));
        verify(mapper, never()).update(any(), any());
    }

    @Test
    void readOfAnotherTenantsBrandIsForbidden() {
        authenticate(TENANT_A, TENANT_A);
        when(mapper.selectById("brand-b")).thenReturn(ownedBrand(TENANT_B));

        assertThrows(AccessDeniedException.class, () -> service.getBrand("brand-b"));
    }

    private static ProdBrand ownedBrand(UUID tenantId) {
        ProdBrand brand = new ProdBrand();
        brand.setTenantId(tenantId.toString());
        return brand;
    }
}
