package com.scmcloud.product.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.product.domain.entity.ProdSpu;
import com.scmcloud.product.mapper.ProdSpuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProdSpuQueryService {

    private final ProdSpuMapper prodSpuMapper;

    @Slave
    public ProdSpu getById(String id) {
        return prodSpuMapper.selectById(id);
    }

    @Slave
    public List<ProdSpu> listByCategoryId(String categoryId) {
        return prodSpuMapper.selectList(
                Wrappers.<ProdSpu>lambdaQuery()
                        .eq(ProdSpu::getCategoryId, categoryId)
                        .eq(ProdSpu::getDeleted, false)
                        .ne(ProdSpu::getStatus, 3)
                        .orderByAsc(ProdSpu::getSortOrder)
        );
    }

    @Slave
    public List<ProdSpu> listByBrandId(String brandId) {
        return prodSpuMapper.selectList(
                Wrappers.<ProdSpu>lambdaQuery()
                        .eq(ProdSpu::getBrandId, brandId)
                        .eq(ProdSpu::getDeleted, false)
                        .ne(ProdSpu::getStatus, 3)
                        .orderByAsc(ProdSpu::getSortOrder)
        );
    }

    @Slave
    public Page<ProdSpu> search(String keyword, String categoryId, String brandId, Integer status,
                                Integer page, Integer size) {
        LambdaQueryWrapper<ProdSpu> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ProdSpu::getDeleted, false);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(ProdSpu::getSpuName, keyword)
                    .or()
                    .like(ProdSpu::getSpuCode, keyword)
            );
        }
        if (StringUtils.hasText(categoryId)) {
            wrapper.eq(ProdSpu::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(brandId)) {
            wrapper.eq(ProdSpu::getBrandId, brandId);
        }
        if (status != null) {
            wrapper.eq(ProdSpu::getStatus, status);
        } else {
            wrapper.ne(ProdSpu::getStatus, 3);
        }

        wrapper.orderByDesc(ProdSpu::getCreateTime);

        return prodSpuMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
