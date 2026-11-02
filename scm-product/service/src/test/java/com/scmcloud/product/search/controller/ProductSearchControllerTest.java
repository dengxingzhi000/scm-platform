package com.scmcloud.product.search.controller;

import com.scmcloud.product.search.service.ProductSearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductSearchControllerTest {

    @Mock
    private ProductSearchService productSearchService;

    @InjectMocks
    private ProductSearchController productSearchController;

    @Test
    void quickSearchShouldDelegateRequestAssemblyToService() throws Exception {
        when(productSearchService.quickSearch("phone", 1, 20, "sales", "desc")).thenReturn(Page.empty());

        MockMvc mvc = MockMvcBuilders.standaloneSetup(productSearchController).build();
        mvc.perform(get("/api/v1/products/search").param("keyword", "phone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(productSearchService).quickSearch("phone", 1, 20, "sales", "desc");
    }
}
