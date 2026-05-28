package scm.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import scm.product.domain.entity.ProdSpu;
import scm.product.mapper.ProdSpuMapper;
import scm.product.service.IProdSpuService;

import java.util.List;

@Slf4j
@Service
public class ProdSpuServiceImpl extends ServiceImpl<ProdSpuMapper, ProdSpu> implements IProdSpuService {

    @Override
    public List<ProdSpu> listByCategoryId(String categoryId) {
        log.debug("查询分类SPU: categoryId={}", categoryId);
        return lambdaQuery()
                .eq(ProdSpu::getCategoryId, categoryId)
                .eq(ProdSpu::getDeleted, false)
                .ne(ProdSpu::getStatus, 3)
                .orderByAsc(ProdSpu::getSortOrder)
                .list();
    }

    @Override
    public List<ProdSpu> listByBrandId(String brandId) {
        log.debug("查询品牌SPU: brandId={}", brandId);
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
        log.debug("搜索SPU: keyword={}, categoryId={}, brandId={}, status={}", keyword, categoryId, brandId, status);

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
}
