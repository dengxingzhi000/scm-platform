package scm.purchase.service.impl

-purchase/service/src/main/java.service.impl;

import scm-purchase/service/src/main/java.domain.entity.PurQuotationItem;
import scm-purchase/service/src/main/java.mapper.PurQuotationItemMapper;
import scm-purchase/service/src/main/java.service.IPurQuotationItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import scm.purchase.service.IPurQuotationItemService;

/**
 * <p>
 * 供应商报价明细表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class PurQuotationItemServiceImpl extends ServiceImpl<PurQuotationItemMapper, PurQuotationItem> implements IPurQuotationItemService {

}
