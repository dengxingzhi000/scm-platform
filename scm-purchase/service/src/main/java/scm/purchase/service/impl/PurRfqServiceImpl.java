package scm.purchase.service.impl

-purchase/service/src/main/java.service.impl;

import scm-purchase/service/src/main/java.domain.entity.PurRfq;
import scm-purchase/service/src/main/java.mapper.PurRfqMapper;
import scm-purchase/service/src/main/java.service.IPurRfqService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import scm.purchase.service.IPurRfqService;

/**
 * <p>
 * 询价单表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class PurRfqServiceImpl extends ServiceImpl<PurRfqMapper, PurRfq> implements IPurRfqService {

}
