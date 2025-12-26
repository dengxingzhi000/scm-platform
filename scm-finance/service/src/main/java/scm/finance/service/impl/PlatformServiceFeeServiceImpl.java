package scm.finance.service.impl;

import scm.finance.domain.entity.PlatformServiceFee;
import scm.finance.mapper.PlatformServiceFeeMapper;
import scm.finance.service.IPlatformServiceFeeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 平台服务费表（SaaS平台） 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Service
public class PlatformServiceFeeServiceImpl extends ServiceImpl<PlatformServiceFeeMapper, PlatformServiceFee>
        implements IPlatformServiceFeeService {

}
