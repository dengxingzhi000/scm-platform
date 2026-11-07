package com.cognora.platform.system.service.query;

import com.cognora.platform.common.data.rw.annotation.Slave;
import com.cognora.platform.common.domain.PageResult;
import com.cognora.platform.common.dto.user.LoginHistoryDTO;
import com.cognora.platform.common.exception.BusinessException;
import com.cognora.platform.common.response.ResultCode;
import com.cognora.platform.common.tenant.TenantValidationUtil;
import com.cognora.platform.system.mapper.UserLoginHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 用户登录历史查询。默认最近 30 天，单次最大 180 天，避免全分区扫描。
 */
@Service
@RequiredArgsConstructor
public class UserLoginHistoryQueryService {

    private static final int DEFAULT_DAYS = 30;
    private static final int MAX_DAYS = 180;
    private static final int MAX_PAGE_SIZE = 100;

    private final UserLoginHistoryMapper loginHistoryMapper;

    @Slave
    public PageResult<LoginHistoryDTO> query(UUID userId,
                                             LocalDateTime startTime,
                                             LocalDateTime endTime,
                                             String operationType,
                                             Integer page,
                                             Integer size) {
        UUID tenantId = TenantValidationUtil.getRequiredTenantId();

        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        LocalDateTime start = startTime != null ? startTime : end.minusDays(DEFAULT_DAYS);
        if (start.isAfter(end)) {
            throw new BusinessException(ResultCode.VALIDATION_FAILED.getCode(),
                    "开始时间不能晚于结束时间");
        }
        if (Duration.between(start, end).toDays() > MAX_DAYS) {
            throw new BusinessException(ResultCode.VALIDATION_FAILED.getCode(),
                    "查询时间范围不能超过180天");
        }

        int pageNum = page != null && page > 0 ? page : 1;
        int pageSize = size != null ? Math.min(size, MAX_PAGE_SIZE) : 20;
        if (pageSize <= 0) {
            pageSize = 20;
        }

        long total = loginHistoryMapper.count(tenantId, userId, start, end, operationType);
        List<LoginHistoryDTO> records = loginHistoryMapper.selectPage(
                tenantId, userId, start, end, operationType,
                (long) (pageNum - 1) * pageSize, pageSize);

        long pages = pageSize > 0 ? (total + pageSize - 1) / pageSize : 0;
        return PageResult.<LoginHistoryDTO>builder()
                .total(total)
                .pageNum((long) pageNum)
                .pageSize((long) pageSize)
                .pages(pages)
                .records(records)
                .build();
    }
}
