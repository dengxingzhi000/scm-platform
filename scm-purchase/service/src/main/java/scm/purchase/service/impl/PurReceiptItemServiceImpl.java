package scm.purchase.service.impl

-purchase/service/src/main/java.service.impl;

import scm-purchase/service/src/main/java.domain.entity.PurReceiptItem;
import scm-purchase/service/src/main/java.mapper.PurReceiptItemMapper;
import scm-purchase/service/src/main/java.service.IPurReceiptItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import scm.purchase.service.IPurReceiptItemService;

/**
 * <p>
 * 采购入库明细表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class PurReceiptItemServiceImpl extends ServiceImpl<PurReceiptItemMapper, PurReceiptItem> implements IPurReceiptItemService {

}
