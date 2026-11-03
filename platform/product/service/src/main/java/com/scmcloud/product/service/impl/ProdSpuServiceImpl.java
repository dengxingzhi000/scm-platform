package com.scmcloud.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.scmcloud.product.domain.entity.ProdSpu;
import com.scmcloud.product.mapper.ProdSpuMapper;
import com.scmcloud.product.service.IProdSpuService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ProdSpuServiceImpl extends ServiceImpl<ProdSpuMapper, ProdSpu> implements IProdSpuService {

    @Override
    public List<ProdSpu> listByCategoryId(String categoryId) {
        log.debug("查询分类SPU: categoryId={}", (categoryId + "").replace("\r", "_").replace("\n", "_"));
        return lambdaQuery()
                .eq(ProdSpu::getCategoryId, categoryId)
                .eq(ProdSpu::getDeleted, false)
                .ne(ProdSpu::getStatus, 3)
                .orderByAsc(ProdSpu::getSortOrder)
                .list();
    }

    @Override
    public List<ProdSpu> listByBrandId(String brandId) {
        log.debug("查询品牌SPU: brandId={}", (brandId + "").replace("\r", "_").replace("\n", "_"));
        return lambdaQuery()
                .eq(ProdSpu::getBrandId, brandId)
                .eq(ProdSpu::getDeleted, false)
                .ne(ProdSpu::getStatus, 3)
                .orderByAsc(ProdSpu::getSortOrder)
                .list();
    }

    @Override
    public Page<ProdSpu> search(String keyword, String categoryId, String brandId, Integer status,
                                Integer page, Integer size) {
        log.debug("搜索SPU: keyword={}, categoryId={}, brandId={}, status={}", (keyword + "").replace("\r", "_").replace("\n", "_"), (categoryId + "").replace("\r", "_").replace("\n", "_"), (brandId + "").replace("\r", "_").replace("\n", "_"), (status + "").replace("\r", "_").replace("\n", "_"));

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

        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public ProdSpu createSpu(ProdSpu spu) {
        spu.setId(UUID.randomUUID().toString());
        spu.setCreateTime(LocalDateTime.now());
        spu.setUpdateTime(LocalDateTime.now());
        spu.setDeleted(false);
        if (spu.getStatus() == null) {
            spu.setStatus(0);
        }
        save(spu);
        return spu;
    }

    @Override
    public void updateSpu(String id, ProdSpu spu) {
        spu.setId(id);
        spu.setUpdateTime(LocalDateTime.now());
        updateById(spu);
    }

    @Override
    public void deleteSpu(String id) {
        lambdaUpdate()
                .eq(ProdSpu::getId, id)
                .set(ProdSpu::getDeleted, true)
                .set(ProdSpu::getStatus, 3)
                .set(ProdSpu::getUpdateTime, LocalDateTime.now())
                .update();
    }
}
