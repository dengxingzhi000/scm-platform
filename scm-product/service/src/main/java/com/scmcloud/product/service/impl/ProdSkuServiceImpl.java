package com.scmcloud.product.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.scmcloud.product.domain.entity.ProdSku;
import com.scmcloud.product.mapper.ProdSkuMapper;
import com.scmcloud.product.service.IProdSkuService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ProdSkuServiceImpl extends ServiceImpl<ProdSkuMapper, ProdSku> implements IProdSkuService {

    @Override
    public List<ProdSku> listBySpuId(String spuId) {
        log.debug("查询SPU的SKU列表: spuId={}", (spuId + "").replace("\r", "_").replace("\n", "_"));
        return lambdaQuery()
                .eq(ProdSku::getSpuId, spuId)
                .eq(ProdSku::getDeleted, false)
                .ne(ProdSku::getStatus, 3)
                .orderByAsc(ProdSku::getCreateTime)
                .list();
    }

    @Override
    public List<ProdSku> listBySpuIds(List<String> spuIds) {
        log.debug("批量查询SPU的SKU列表: spuIds.size={}", spuIds.size());
        return lambdaQuery()
                .in(ProdSku::getSpuId, spuIds)
                .eq(ProdSku::getDeleted, false)
                .ne(ProdSku::getStatus, 3)
                .orderByAsc(ProdSku::getCreateTime)
                .list();
    }

    @Override
    public Page<ProdSku> pageBySpuId(String spuId, Integer page, Integer size) {
        return lambdaQuery()
                .eq(spuId != null, ProdSku::getSpuId, spuId)
                .eq(ProdSku::getDeleted, false)
                .ne(ProdSku::getStatus, 3)
                .orderByDesc(ProdSku::getCreateTime)
                .page(new Page<>(page, size));
    }

    @Override
    public ProdSku createSku(ProdSku sku) {
        sku.setId(UUID.randomUUID().toString());
        sku.setCreateTime(LocalDateTime.now());
        sku.setUpdateTime(LocalDateTime.now());
        sku.setDeleted(false);
        if (sku.getStatus() == null) {
            sku.setStatus(1);
        }
        save(sku);
        return sku;
    }

    @Override
    public void updateSku(String id, ProdSku sku) {
        sku.setId(id);
        sku.setUpdateTime(LocalDateTime.now());
        updateById(sku);
    }

    @Override
    public void deleteSku(String id) {
        lambdaUpdate()
                .eq(ProdSku::getId, id)
                .set(ProdSku::getDeleted, true)
                .set(ProdSku::getStatus, 3)
                .set(ProdSku::getUpdateTime, LocalDateTime.now())
                .update();
    }
}
