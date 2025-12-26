package scm.purchase.service.impl

-purchase/service/src/main/java.service.impl;

import scm-purchase/service/src/main/java.domain.entity.PurRequestItem;
import scm-purchase/service/src/main/java.mapper.PurRequestItemMapper;
import scm-purchase/service/src/main/java.service.IPurRequestItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import scm.purchase.service.IPurRequestItemService;

/**
 * <p>
 * 采购申请明细表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class PurRequestItemServiceImpl extends ServiceImpl<PurRequestItemMapper, PurRequestItem> implements IPurRequestItemService {

}
