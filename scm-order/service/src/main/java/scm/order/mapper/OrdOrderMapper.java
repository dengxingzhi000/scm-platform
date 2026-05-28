package scm.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import scm.order.domain.entity.OrdOrder;

@Mapper
public interface OrdOrderMapper extends BaseMapper<OrdOrder> {
}
