package com.cognora.platform.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.cognora.platform.product.outbox.ProdOutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.cognora.platform.product.domain.entity.ProdSpu;
import com.cognora.platform.product.mapper.ProdSpuMapper;
import com.cognora.platform.product.service.IProdSpuService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProdSpuServiceImpl extends ServiceImpl<ProdSpuMapper, ProdSpu> implements IProdSpuService {

    private final ProdOutboxService prodOutboxService;

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
    @Transactional(rollbackFor = Exception.class)
    public ProdSpu createSpu(ProdSpu spu) {
        spu.setId(UUID.randomUUID().toString());
        spu.setCreateTime(LocalDateTime.now());
        spu.setUpdateTime(LocalDateTime.now());
        spu.setDeleted(false);
        if (spu.getStatus() == null) {
            spu.setStatus(0);
        }
        save(spu);
        // outbox 事件必须在同一事务内写入：这里抛出的任何异常都会回滚上面的 insert。
        prodOutboxService.recordSpuUpsert(requirePersisted(spu.getId()), true);
        return spu;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSpu(String id, ProdSpu spu) {
        spu.setId(id);
        spu.setUpdateTime(LocalDateTime.now());
        boolean updated = updateById(spu);
        if (!updated) {
            log.warn("更新 SPU 未命中，跳过事件: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
            return;
        }
        // 重新读取整行：请求体通常是部分字段，直接用入参会把未提交字段写成 null 覆盖 ES 文档。
        prodOutboxService.recordSpuUpsert(requirePersisted(id), false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSpu(String id) {
        // 逻辑删除后行仍然存在，但消费者只从 before 里取 id 与 tenant_id，所以先读出来。
        ProdSpu existing = getById(id);
        lambdaUpdate()
                .eq(ProdSpu::getId, id)
                .set(ProdSpu::getDeleted, true)
                .set(ProdSpu::getStatus, 3)
                .set(ProdSpu::getUpdateTime, LocalDateTime.now())
                .update();
        if (existing == null) {
            log.warn("删除 SPU 未命中，跳过事件: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
            return;
        }
        prodOutboxService.recordSpuDelete(existing);
    }

    private ProdSpu requirePersisted(String id) {
        ProdSpu persisted = getById(id);
        if (persisted == null) {
            throw new IllegalStateException("SPU 写入成功但无法回读，无法生成索引事件: " + id);
        }
        return persisted;
    }
}
