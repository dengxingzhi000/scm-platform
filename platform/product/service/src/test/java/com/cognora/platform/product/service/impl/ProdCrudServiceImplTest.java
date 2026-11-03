package com.cognora.platform.product.service.impl;

import com.cognora.platform.product.domain.entity.ProdAttributeTemplate;
import com.cognora.platform.product.domain.entity.ProdBrand;
import com.cognora.platform.product.domain.entity.ProdCategory;
import com.cognora.platform.product.domain.entity.ProdSku;
import com.cognora.platform.product.domain.entity.ProdSpu;
import com.cognora.platform.product.mapper.ProdAttributeTemplateMapper;
import com.cognora.platform.product.mapper.ProdBrandMapper;
import com.cognora.platform.product.mapper.ProdCategoryMapper;
import com.cognora.platform.product.mapper.ProdSkuMapper;
import com.cognora.platform.product.mapper.ProdSpuMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProdCrudServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, ProdSpu.class);
        TableInfoHelper.initTableInfo(assistant, ProdSku.class);
        TableInfoHelper.initTableInfo(assistant, ProdBrand.class);
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
        verify(mapper).insert(spu);
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

        service.deleteSpu("spu-1");

        ArgumentCaptor<LambdaUpdateWrapper<ProdSpu>> captor = ArgumentCaptor.captor();
        verify(mapper).update(isNull(), captor.capture());
        LambdaUpdateWrapper<ProdSpu> wrapper = captor.getValue();
        assertTrue(wrapper.getSqlSet().contains("deleted"));
        assertTrue(wrapper.getSqlSet().contains("status"));
        assertTrue(wrapper.getSqlSet().contains("update_time"));
        assertTrue(wrapper.getSqlSegment().contains("id"));
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
    void brandDeleteShouldIssueLogicalUpdate() {
        ProdBrandMapper mapper = mock(ProdBrandMapper.class);
        ProdBrandServiceImpl service = new ProdBrandServiceImpl();
        attach(service, mapper);

        service.deleteBrand("brand-1");

        ArgumentCaptor<LambdaUpdateWrapper<ProdBrand>> captor = ArgumentCaptor.captor();
        verify(mapper).update(isNull(), captor.capture());
        LambdaUpdateWrapper<ProdBrand> wrapper = captor.getValue();
        assertTrue(wrapper.getSqlSet().contains("deleted"));
        assertTrue(wrapper.getSqlSet().contains("update_time"));
        assertTrue(wrapper.getSqlSegment().contains("id"));
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
