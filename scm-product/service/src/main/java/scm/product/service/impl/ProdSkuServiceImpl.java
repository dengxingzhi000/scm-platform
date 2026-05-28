package scm.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import scm.product.domain.entity.ProdSku;
import scm.product.mapper.ProdSkuMapper;
import scm.product.service.IProdSkuService;

import java.util.List;

@Slf4j
@Service
public class ProdSkuServiceImpl extends ServiceImpl<ProdSkuMapper, ProdSku> implements IProdSkuService {

    @Override
    public List<ProdSku> listBySpuId(String spuId) {
        log.debug("查询SPU的SKU列表: spuId={}", spuId);
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
}
