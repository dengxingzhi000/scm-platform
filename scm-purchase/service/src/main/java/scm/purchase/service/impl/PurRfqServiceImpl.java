package scm.purchase.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import scm.purchase.domain.entity.PurRfq;
import scm.purchase.mapper.PurRfqMapper;
import scm.purchase.service.IPurRfqService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class PurRfqServiceImpl extends ServiceImpl<PurRfqMapper, PurRfq> implements IPurRfqService {

    @Override
    public PurRfq getByRfqNo(String rfqNo) {
        return lambdaQuery()
                .eq(PurRfq::getRfqNo, rfqNo)
                .eq(PurRfq::getDeleted, false)
                .one();
    }

    @Override
    public Page<PurRfq> pageQuery(int page, int size, Integer status, Integer rfqType, String keyword) {
        LambdaQueryWrapper<PurRfq> wrapper = Wrappers.lambdaQuery();
        if (status != null) {
            wrapper.eq(PurRfq::getStatus, status);
        }
        if (rfqType != null) {
            wrapper.eq(PurRfq::getRfqType, rfqType);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(PurRfq::getRfqNo, keyword)
                    .or()
                    .like(PurRfq::getRfqTitle, keyword));
        }
        wrapper.eq(PurRfq::getDeleted, false);
        wrapper.orderByDesc(PurRfq::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public List<PurRfq> listByStatus(Integer status) {
        return lambdaQuery()
                .eq(PurRfq::getStatus, status)
                .eq(PurRfq::getDeleted, false)
                .orderByDesc(PurRfq::getCreateTime)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean publish(String id) {
        PurRfq rfq = getById(id);
        if (rfq == null || rfq.getDeleted()) {
            throw new IllegalArgumentException("询价单不存在: " + id);
        }
        if (rfq.getStatus() != 0) {
            throw new IllegalStateException("只有草稿状态的询价单才能发布");
        }
        rfq.setStatus(1);
        rfq.setUpdateTime(LocalDateTime.now());
        return updateById(rfq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean close(String id) {
        PurRfq rfq = getById(id);
        if (rfq == null || rfq.getDeleted()) {
            throw new IllegalArgumentException("询价单不存在: " + id);
        }
        if (rfq.getStatus() == 4) {
            throw new IllegalStateException("询价单已关闭");
        }
        rfq.setStatus(4);
        rfq.setUpdateTime(LocalDateTime.now());
        return updateById(rfq);
    }
}
