package scm.purchase.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import scm.purchase.domain.entity.PurRequest;
import scm.purchase.mapper.PurRequestMapper;
import scm.purchase.service.IPurRequestService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class PurRequestServiceImpl extends ServiceImpl<PurRequestMapper, PurRequest> implements IPurRequestService {

    @Override
    public PurRequest getByRequestNo(String requestNo) {
        return lambdaQuery()
                .eq(PurRequest::getRequestNo, requestNo)
                .eq(PurRequest::getDeleted, false)
                .one();
    }

    @Override
    public Page<PurRequest> pageQuery(int page, int size, Integer status, Integer requestType, String keyword) {
        LambdaQueryWrapper<PurRequest> wrapper = Wrappers.lambdaQuery();
        if (status != null) {
            wrapper.eq(PurRequest::getStatus, status);
        }
        if (requestType != null) {
            wrapper.eq(PurRequest::getRequestType, requestType);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(PurRequest::getRequestNo, keyword)
                    .or()
                    .like(PurRequest::getRequesterName, keyword)
                    .or()
                    .like(PurRequest::getPurpose, keyword));
        }
        wrapper.eq(PurRequest::getDeleted, false);
        wrapper.orderByDesc(PurRequest::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public List<PurRequest> listByStatus(Integer status) {
        return lambdaQuery()
                .eq(PurRequest::getStatus, status)
                .eq(PurRequest::getDeleted, false)
                .orderByDesc(PurRequest::getCreateTime)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submit(String id) {
        PurRequest request = getById(id);
        if (request == null || request.getDeleted()) {
            throw new IllegalArgumentException("采购申请不存在: " + id);
        }
        if (request.getStatus() != 0) {
            throw new IllegalStateException("只有草稿状态的申请才能提交");
        }
        request.setStatus(1);
        request.setSubmittedAt(LocalDateTime.now());
        request.setUpdateTime(LocalDateTime.now());
        return updateById(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approve(String id, String approverId, String approverName) {
        PurRequest request = getById(id);
        if (request == null || request.getDeleted()) {
            throw new IllegalArgumentException("采购申请不存在: " + id);
        }
        if (request.getStatus() != 1) {
            throw new IllegalStateException("只有待审批状态的申请才能审批");
        }
        request.setStatus(2);
        request.setCurrentApproverId(approverId);
        request.setCurrentApproverName(approverName);
        request.setApprovedAt(LocalDateTime.now());
        request.setUpdateTime(LocalDateTime.now());
        return updateById(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reject(String id, String approverId, String approverName, String reason) {
        PurRequest request = getById(id);
        if (request == null || request.getDeleted()) {
            throw new IllegalArgumentException("采购申请不存在: " + id);
        }
        if (request.getStatus() != 1) {
            throw new IllegalStateException("只有待审批状态的申请才能驳回");
        }
        request.setStatus(3);
        request.setCurrentApproverId(approverId);
        request.setCurrentApproverName(approverName);
        request.setRejectedAt(LocalDateTime.now());
        request.setRejectReason(reason);
        request.setUpdateTime(LocalDateTime.now());
        return updateById(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean close(String id) {
        PurRequest request = getById(id);
        if (request == null || request.getDeleted()) {
            throw new IllegalArgumentException("采购申请不存在: " + id);
        }
        if (request.getStatus() == 4) {
            throw new IllegalStateException("已转采购单的申请不能关闭");
        }
        request.setStatus(5);
        request.setUpdateTime(LocalDateTime.now());
        return updateById(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean convertToOrder(String id, String orderId, String orderNo) {
        PurRequest request = getById(id);
        if (request == null || request.getDeleted()) {
            throw new IllegalArgumentException("采购申请不存在: " + id);
        }
        if (request.getStatus() != 2) {
            throw new IllegalStateException("只有已审批的申请才能转采购单");
        }
        if (Boolean.TRUE.equals(request.getConverted())) {
            throw new IllegalStateException("该申请已转采购单");
        }
        request.setStatus(4);
        request.setConverted(true);
        request.setPurchaseOrderId(orderId);
        request.setPurchaseOrderNo(orderNo);
        request.setUpdateTime(LocalDateTime.now());
        return updateById(request);
    }
}
