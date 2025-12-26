package scm.purchase.service.impl

-purchase/service/src/main/java.service.impl;

import scm-purchase/service/src/main/java.domain.entity.PurRfqItem;
import scm-purchase/service/src/main/java.mapper.PurRfqItemMapper;
import scm-purchase/service/src/main/java.service.IPurRfqItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import scm.purchase.service.IPurRfqItemService;

/**
 * <p>
 * 询价单明细表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class PurRfqItemServiceImpl extends ServiceImpl<PurRfqItemMapper, PurRfqItem> implements IPurRfqItemService {

}
