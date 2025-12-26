package scm.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import scm.product.domain.entity.ProdSku;

/**
 * <p>
 * SKU库存单位表 Mapper 接口
 * </p>
 *
 * @author author
 * @since 2025-12-25
 */
@Mapper
public interface ProdSkuMapper extends BaseMapper<ProdSku> {

}
