package scm.supplier.service.impl;

import scm.supplier.domain.entity.SupPurchaseOrderItem;
import scm.supplier.mapper.SupPurchaseOrderItemMapper;
import scm.supplier.service.ISupPurchaseOrderItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 采购单明细表 服务实现类
 * </p>
 *
 * @author author
 * @since 2025-12-25
 */
@Service
public class SupPurchaseOrderItemServiceImpl extends ServiceImpl<SupPurchaseOrderItemMapper, SupPurchaseOrderItem>
        implements ISupPurchaseOrderItemService {

}
