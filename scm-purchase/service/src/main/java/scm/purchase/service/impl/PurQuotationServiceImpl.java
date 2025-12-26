package scm.purchase.service.impl

-purchase/service/src/main/java.service.impl;

import scm-purchase/service/src/main/java.domain.entity.PurQuotation;
import scm-purchase/service/src/main/java.mapper.PurQuotationMapper;
import scm-purchase/service/src/main/java.service.IPurQuotationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import scm.purchase.service.IPurQuotationService;

/**
 * <p>
 * 供应商报价单表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class PurQuotationServiceImpl extends ServiceImpl<PurQuotationMapper, PurQuotation> implements IPurQuotationService {

}
