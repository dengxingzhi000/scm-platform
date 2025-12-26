package scm.purchase.service.impl

-purchase/service/src/main/java.service.impl;

import scm-purchase/service/src/main/java.domain.entity.PurReceipt;
import scm-purchase/service/src/main/java.mapper.PurReceiptMapper;
import scm-purchase/service/src/main/java.service.IPurReceiptService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import scm.purchase.service.IPurReceiptService;

/**
 * <p>
 * 采购入库单表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class PurReceiptServiceImpl extends ServiceImpl<PurReceiptMapper, PurReceipt> implements IPurReceiptService {

}
