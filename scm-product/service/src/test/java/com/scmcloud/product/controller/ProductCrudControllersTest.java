package com.scmcloud.product.controller;

import com.scmcloud.product.service.IProdAttributeTemplateService;
import com.scmcloud.product.service.IProdBrandService;
import com.scmcloud.product.service.IProdCategoryService;
import com.scmcloud.product.service.IProdSkuService;
import com.scmcloud.product.service.IProdSpuService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductCrudControllersTest {

    @Mock
    private IProdSpuService spuService;
    @Mock
    private IProdSkuService skuService;
    @Mock
    private IProdBrandService brandService;
    @Mock
    private IProdCategoryService categoryService;
    @Mock
    private IProdAttributeTemplateService attributeTemplateService;

    @InjectMocks
    private ProdSpuController spuController;
    @InjectMocks
    private ProdSkuController skuController;
    @InjectMocks
    private ProdBrandController brandController;
    @InjectMocks
    private ProdCategoryController categoryController;
    @InjectMocks
    private ProdAttributeTemplateController attributeTemplateController;

    @Test
    void spuCreateAndDeleteShouldDelegate() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(spuController).build();

        mvc.perform(post("/api/v1/prod-spu").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(spuService).createSpu(any());

        mvc.perform(delete("/api/v1/prod-spu/spu-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(spuService).deleteSpu("spu-1");
    }

    @Test
    void skuCreateAndDeleteShouldDelegate() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(skuController).build();

        mvc.perform(post("/api/v1/prod-sku").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(skuService).createSku(any());

        mvc.perform(delete("/api/v1/prod-sku/sku-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(skuService).deleteSku("sku-1");
    }

    @Test
    void brandCreateAndDeleteShouldDelegate() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(brandController).build();

        mvc.perform(post("/api/v1/prod-brand").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(brandService).createBrand(any());

        mvc.perform(delete("/api/v1/prod-brand/brand-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(brandService).deleteBrand("brand-1");
    }

    @Test
    void categoryCreateAndDeleteShouldDelegate() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(categoryController).build();

        mvc.perform(post("/api/v1/prod-category").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(categoryService).createCategory(any());

        mvc.perform(delete("/api/v1/prod-category/category-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(categoryService).deleteCategory("category-1");
    }

    @Test
    void attributeTemplateCreateAndDeleteShouldDelegate() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(attributeTemplateController).build();

        mvc.perform(post("/api/v1/prod-attribute-template").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(attributeTemplateService).createTemplate(any());

        mvc.perform(delete("/api/v1/prod-attribute-template/template-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(attributeTemplateService).deleteTemplate("template-1");
    }

    @Test
    void listEndpointsShouldDelegateQueryConditions() throws Exception {
        MockMvc skuMvc = MockMvcBuilders.standaloneSetup(skuController).build();
        skuMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/prod-sku").param("spuId", "spu-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(skuService).pageBySpuId(eq("spu-1"), eq(1), eq(10));

        MockMvc brandMvc = MockMvcBuilders.standaloneSetup(brandController).build();
        brandMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/prod-brand").param("name", "brand"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(brandService).pageByName(eq("brand"), eq(1), eq(10));
    }
}
