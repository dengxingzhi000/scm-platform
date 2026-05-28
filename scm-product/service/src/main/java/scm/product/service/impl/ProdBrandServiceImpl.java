package scm.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import scm.product.domain.entity.ProdBrand;
import scm.product.mapper.ProdBrandMapper;
import scm.product.service.IProdBrandService;

import java.util.List;

@Slf4j
@Service
public class ProdBrandServiceImpl extends ServiceImpl<ProdBrandMapper, ProdBrand> implements IProdBrandService {

    @Override
    public List<ProdBrand> searchByName(String name) {
        log.debug("搜索品牌: name={}", name);
        return lambdaQuery()
                .like(StringUtils.hasText(name), ProdBrand::getBrandName, name)
                .eq(ProdBrand::getDeleted, false)
                .eq(ProdBrand::getEnabled, true)
                .orderByAsc(ProdBrand::getSortOrder)
                .list();
    }

    @Override
    public List<ProdBrand> listFeatured() {
        log.debug("查询推荐品牌");
        return lambdaQuery()
                .eq(ProdBrand::getFeatured, true)
                .eq(ProdBrand::getDeleted, false)
                .eq(ProdBrand::getEnabled, true)
                .orderByAsc(ProdBrand::getSortOrder)
                .list();
    }
}
