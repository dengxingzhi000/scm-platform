package com.scmcloud.product.service.impl;

import com.scmcloud.product.domain.entity.ProdAttributeTemplate;
import com.scmcloud.product.domain.entity.ProdBrand;
import com.scmcloud.product.domain.entity.ProdCategory;
import com.scmcloud.product.domain.entity.ProdSku;
import com.scmcloud.product.domain.entity.ProdSpu;
import com.scmcloud.product.mapper.ProdAttributeTemplateMapper;
import com.scmcloud.product.mapper.ProdBrandMapper;
import com.scmcloud.product.mapper.ProdCategoryMapper;
import com.scmcloud.product.mapper.ProdSkuMapper;
import com.scmcloud.product.mapper.ProdSpuMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProdCrudServiceImplTest {

    @BeforeEach
    void initTableInfo() {
        // The delete paths build a LambdaUpdateWrapper, which resolves the
        // column names through the MyBatis-Plus lambda cache. That cache is
        // only populated when a mapper is initialised, so a plain Mockito
        // test has to seed it explicitly.
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, ProdSpu.class);
        TableInfoHelper.initTableInfo(assistant, ProdBrand.class);
        TableInfoHelper.initTableInfo(assistant, ProdSku.class);
        TableInfoHelper.initTableInfo(assistant, ProdCategory.class);
        TableInfoHelper.initTableInfo(assistant, ProdAttributeTemplate.class);
    }

    private void attach(Object service, Object mapper) {
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    void spuCreateShouldInitializeFieldsAndDefaultToDraft() {
        ProdSpuMapper mapper = mock(ProdSpuMapper.class);
        ProdSpuServiceImpl service = new ProdSpuServiceImpl();
        attach(service, mapper);
        when(mapper.insert(any(ProdSpu.class))).thenReturn(1);

        ProdSpu spu = new ProdSpu();
        ProdSpu created = service.createSpu(spu);

        assertNotNull(created.getId());
        assertNotNull(created.getCreateTime());
        assertNotNull(created.getUpdateTime());
        assertFalse(created.getDeleted());
        assertEquals(0, created.getStatus());
        verify(mapper).insert(spu);
    }

    @Test
    void spuCreateShouldKeepProvidedStatus() {
        ProdSpuMapper mapper = mock(ProdSpuMapper.class);
        ProdSpuServiceImpl service = new ProdSpuServiceImpl();
        attach(service, mapper);

        ProdSpu spu = new ProdSpu();
        spu.setStatus(2);
        service.createSpu(spu);

        assertEquals(2, spu.getStatus());
    }

    @Test
    void spuUpdateShouldSetIdAndRefreshTime() {
        ProdSpuMapper mapper = mock(ProdSpuMapper.class);
        ProdSpuServiceImpl service = new ProdSpuServiceImpl();
        attach(service, mapper);

        ProdSpu spu = new ProdSpu();
        spu.setId("old-id");
        service.updateSpu("new-id", spu);

        assertEquals("new-id", spu.getId());
        assertNotNull(spu.getUpdateTime());
        verify(mapper).updateById(spu);
    }

    @Test
    void spuDeleteShouldIssueLogicalUpdate() {
        ProdSpuMapper mapper = mock(ProdSpuMapper.class);
        ProdSpuServiceImpl service = new ProdSpuServiceImpl();
        attach(service, mapper);
        when(mapper.update(isNull(), any())).thenReturn(1);

        service.deleteSpu("spu-1");

        verify(mapper).update(isNull(), any());
    }

    @Test
    void skuCreateShouldDefaultToEnabled() {
        ProdSkuMapper mapper = mock(ProdSkuMapper.class);
        ProdSkuServiceImpl service = new ProdSkuServiceImpl();
        attach(service, mapper);

        ProdSku sku = new ProdSku();
        ProdSku created = service.createSku(sku);

        assertNotNull(created.getId());
        assertNotNull(created.getCreateTime());
        assertNotNull(created.getUpdateTime());
        assertFalse(created.getDeleted());
        assertEquals(1, created.getStatus());
        verify(mapper).insert(sku);
    }

    @Test
    void brandCreateShouldInitializeFields() {
        ProdBrandMapper mapper = mock(ProdBrandMapper.class);
        ProdBrandServiceImpl service = new ProdBrandServiceImpl();
        attach(service, mapper);

        ProdBrand brand = new ProdBrand();
        ProdBrand created = service.createBrand(brand);

        assertNotNull(created.getId());
        assertNotNull(created.getCreateTime());
        assertNotNull(created.getUpdateTime());
        assertFalse(created.getDeleted());
        verify(mapper).insert(brand);
    }

    @Test
    void brandDeleteShouldNotUpdateWhenNothingToDo() {
        ProdBrandMapper mapper = mock(ProdBrandMapper.class);
        ProdBrandServiceImpl service = new ProdBrandServiceImpl();
        attach(service, mapper);
        when(mapper.update(isNull(), any())).thenReturn(1);

        service.deleteBrand("brand-1");

        verify(mapper).update(isNull(), any());
    }

    @Test
    void categoryCreateShouldInitializeFields() {
        ProdCategoryMapper mapper = mock(ProdCategoryMapper.class);
        ProdCategoryServiceImpl service = new ProdCategoryServiceImpl();
        attach(service, mapper);

        ProdCategory category = new ProdCategory();
        ProdCategory created = service.createCategory(category);

        assertNotNull(created.getId());
        assertNotNull(created.getCreateTime());
        assertNotNull(created.getUpdateTime());
        assertFalse(created.getDeleted());
        verify(mapper).insert(category);
    }

    @Test
    void attributeTemplateCreateShouldInitializeFields() {
        ProdAttributeTemplateMapper mapper = mock(ProdAttributeTemplateMapper.class);
        ProdAttributeTemplateServiceImpl service = new ProdAttributeTemplateServiceImpl();
        attach(service, mapper);

        ProdAttributeTemplate template = new ProdAttributeTemplate();
        ProdAttributeTemplate created = service.createTemplate(template);

        assertNotNull(created.getId());
        assertNotNull(created.getCreateTime());
        assertNotNull(created.getUpdateTime());
        assertFalse(created.getDeleted());
        verify(mapper).insert(template);
    }

    @Test
    void updateShouldNotInsert() {
        ProdSpuMapper mapper = mock(ProdSpuMapper.class);
        ProdSpuServiceImpl service = new ProdSpuServiceImpl();
        attach(service, mapper);

        service.updateSpu("id", new ProdSpu());

        verify(mapper, never()).insert(any(ProdSpu.class));
    }
}
