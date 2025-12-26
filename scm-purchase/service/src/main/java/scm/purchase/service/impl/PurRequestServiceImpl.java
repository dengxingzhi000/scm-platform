package scm.purchase.service.impl

-purchase/service/src/main/java.service.impl;

import scm-purchase/service/src/main/java.domain.entity.PurRequest;
import scm-purchase/service/src/main/java.mapper.PurRequestMapper;
import scm-purchase/service/src/main/java.service.IPurRequestService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import scm.purchase.service.IPurRequestService;

/**
 * <p>
 * 采购申请单表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class PurRequestServiceImpl extends ServiceImpl<PurRequestMapper, PurRequest> implements IPurRequestService {

}
