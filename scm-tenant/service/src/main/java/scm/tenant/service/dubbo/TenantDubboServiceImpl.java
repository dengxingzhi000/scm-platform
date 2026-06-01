package scm.tenant.service.dubbo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frog.tenant.api.command.TenantCreateCommand;
import com.frog.tenant.api.command.TenantUpdateCommand;
import com.frog.tenant.api.dto.common.PageResult;
import com.frog.tenant.api.dto.tenant.QuotaCheckResultDTO;
import com.frog.tenant.api.dto.tenant.QuotaType;
import com.frog.tenant.api.dto.tenant.TenantDTO;
import com.frog.tenant.api.dto.tenant.TenantResourceQuotaDTO;
import com.frog.tenant.api.query.TenantQuery;
import com.frog.tenant.api.service.TenantDubboService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;
import scm.tenant.domain.entity.Tenant;
import scm.tenant.domain.entity.TenantResourceQuota;
import scm.tenant.service.ITenantFeatureService;
import scm.tenant.service.ITenantResourceQuotaService;
import scm.tenant.service.ITenantService;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@DubboService
@Component
@RequiredArgsConstructor
public class TenantDubboServiceImpl implements TenantDubboService {

    private final ITenantService tenantService;
    private final ITenantFeatureService featureService;
    private final ITenantResourceQuotaService quotaService;

    @Override
    public TenantDTO getById(String tenantId) {
        log.debug("Dubbo查询租户: tenantId={}", tenantId);
        Tenant tenant = tenantService.lambdaQuery()
                .eq(Tenant::getId, tenantId)
                .eq(Tenant::getDeleted, false)
                .one();
        return tenant == null ? null : convertToDTO(tenant);
    }

    @Override
    public TenantDTO getByCode(String tenantCode) {
        log.debug("Dubbo查询租户: tenantCode={}", tenantCode);
        Tenant tenant = tenantService.lambdaQuery()
                .eq(Tenant::getTenantCode, tenantCode)
                .eq(Tenant::getDeleted, false)
                .one();
        return tenant == null ? null : convertToDTO(tenant);
    }

    @Override
    public PageResult<TenantDTO> queryTenants(TenantQuery query) {
        log.debug("Dubbo分页查询租户: query={}", query);

        LambdaQueryWrapper<Tenant> wrapper = Wrappers.lambdaQuery();
        if (query.getTenantName() != null) {
            wrapper.like(Tenant::getTenantName, query.getTenantName());
        }
        if (query.getTenantType() != null) {
            wrapper.eq(Tenant::getTenantType, query.getTenantType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Tenant::getStatus, query.getStatus());
        }
        if (query.getIndustry() != null) {
            wrapper.eq(Tenant::getIndustry, query.getIndustry());
        }
        wrapper.eq(Tenant::getDeleted, false);
        wrapper.orderByDesc(Tenant::getCreateTime);

        Page<Tenant> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<Tenant> result = tenantService.page(page, wrapper);

        PageResult<TenantDTO> pageResult = new PageResult<>();
        pageResult.setTotal(result.getTotal());
        pageResult.setPageNum(query.getPageNum());
        pageResult.setPageSize(query.getPageSize());
        pageResult.setRecords(result.getRecords().stream().map(this::convertToDTO).toList());
        return pageResult;
    }

    @Override
    public boolean checkFeatureEnabled(String tenantId, String featureCode) {
        return featureService.isFeatureEnabled(tenantId, featureCode);
    }

    @Override
    public TenantResourceQuotaDTO getResourceQuota(String tenantId) {
        log.debug("Dubbo查询资源配额: tenantId={}", tenantId);
        TenantResourceQuota quota = quotaService.lambdaQuery()
                .eq(TenantResourceQuota::getTenantId, tenantId)
                .one();
        return quota == null ? null : convertQuotaToDTO(quota);
    }

    @Override
    public QuotaCheckResultDTO checkQuota(String tenantId, QuotaType quotaType, int required) {
        log.debug("Dubbo检查配额: tenantId={}, quotaType={}, required={}", tenantId, quotaType, required);

        TenantResourceQuota quota = quotaService.lambdaQuery()
                .eq(TenantResourceQuota::getTenantId, tenantId)
                .one();

        if (quota == null) {
            return new QuotaCheckResultDTO(false, 0, 0, "配额信息不存在");
        }

        return switch (quotaType) {
            case USER -> buildResult(quota.getCurrentUsers(), quota.getMaxUsers(), required, "用户数");
            case WAREHOUSE -> buildResult(quota.getCurrentWarehouses(), quota.getMaxWarehouses(), required, "仓库数");
            case SKU -> buildResult(quota.getCurrentSkus(), quota.getMaxSkus(), required, "SKU数");
            case ORDER_PER_DAY -> buildResult(quota.getCurrentOrdersToday(), quota.getMaxOrdersPerDay(), required, "每日订单数");
            case STORAGE_GB -> buildResult(quota.getCurrentStorageGb() != null ? quota.getCurrentStorageGb().intValue() : 0, quota.getMaxStorageGb(), required, "存储空间");
            case API_CALLS_PER_DAY -> buildResult(quota.getCurrentApiCallsToday(), quota.getMaxApiCallsPerDay(), required, "每日API调用数");
        };
    }

    private QuotaCheckResultDTO buildResult(int current, int max, int required, String resourceName) {
        boolean available = (current + required) <= max;
        String message = available ? resourceName + "配额充足" : resourceName + "配额不足";
        return new QuotaCheckResultDTO(available, current, max, message);
    }

    @Override
    public String createTenant(TenantCreateCommand command) {
        log.info("Dubbo创建租户: tenantCode={}, tenantName={}", command.getTenantCode(), command.getTenantName());

        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID().toString());
        tenant.setTenantCode(command.getTenantCode());
        tenant.setTenantName(command.getTenantName());
        tenant.setTenantType(command.getTenantType());
        tenant.setCompanyName(command.getCompanyName());
        tenant.setContactName(command.getContactName());
        tenant.setContactPhone(command.getContactPhone());
        tenant.setContactEmail(command.getContactEmail());
        tenant.setIndustry(command.getIndustry());
        tenant.setStatus(0);
        tenant.setDeleted(false);
        tenant.setCreateTime(LocalDateTime.now());
        tenant.setUpdateTime(LocalDateTime.now());

        tenantService.save(tenant);
        log.info("Dubbo创建租户成功: id={}", tenant.getId());
        return tenant.getId();
    }

    @Override
    public void updateTenant(String tenantId, TenantUpdateCommand command) {
        log.info("Dubbo更新租户: tenantId={}", tenantId);

        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setTenantName(command.getTenantName());
        tenant.setCompanyName(command.getCompanyName());
        tenant.setContactName(command.getContactName());
        tenant.setContactPhone(command.getContactPhone());
        tenant.setContactEmail(command.getContactEmail());
        tenant.setIndustry(command.getIndustry());
        tenant.setDomain(command.getDomain());
        tenant.setUpdateTime(LocalDateTime.now());

        tenantService.updateById(tenant);
    }

    @Override
    public void suspendTenant(String tenantId) {
        log.info("Dubbo停用租户: tenantId={}", tenantId);
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setStatus(2);
        tenant.setSuspendedAt(LocalDateTime.now());
        tenant.setUpdateTime(LocalDateTime.now());
        tenantService.updateById(tenant);
    }

    @Override
    public void activateTenant(String tenantId) {
        log.info("Dubbo激活租户: tenantId={}", tenantId);
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setStatus(1);
        tenant.setActivatedAt(LocalDateTime.now());
        tenant.setUpdateTime(LocalDateTime.now());
        tenantService.updateById(tenant);
    }

    private TenantDTO convertToDTO(Tenant tenant) {
        TenantDTO dto = new TenantDTO();
        dto.setId(tenant.getId());
        dto.setTenantCode(tenant.getTenantCode());
        dto.setTenantName(tenant.getTenantName());
        dto.setTenantType(tenant.getTenantType());
        dto.setCompanyName(tenant.getCompanyName());
        dto.setContactName(tenant.getContactName());
        dto.setContactPhone(tenant.getContactPhone());
        dto.setContactEmail(tenant.getContactEmail());
        dto.setStatus(tenant.getStatus());
        dto.setIndustry(tenant.getIndustry());
        dto.setDomain(tenant.getDomain());
        dto.setCreateTime(tenant.getCreateTime());
        return dto;
    }

    private TenantResourceQuotaDTO convertQuotaToDTO(TenantResourceQuota quota) {
        TenantResourceQuotaDTO dto = new TenantResourceQuotaDTO();
        dto.setId(quota.getId());
        dto.setTenantId(quota.getTenantId());
        dto.setMaxUsers(quota.getMaxUsers());
        dto.setCurrentUsers(quota.getCurrentUsers());
        dto.setMaxWarehouses(quota.getMaxWarehouses());
        dto.setCurrentWarehouses(quota.getCurrentWarehouses());
        dto.setMaxSkus(quota.getMaxSkus());
        dto.setCurrentSkus(quota.getCurrentSkus());
        dto.setMaxOrdersPerDay(quota.getMaxOrdersPerDay());
        dto.setCurrentOrdersToday(quota.getCurrentOrdersToday());
        dto.setMaxStorageGb(quota.getMaxStorageGb());
        dto.setCurrentStorageGb(quota.getCurrentStorageGb());
        dto.setMaxApiCallsPerDay(quota.getMaxApiCallsPerDay());
        dto.setCurrentApiCallsToday(quota.getCurrentApiCallsToday());
        return dto;
    }
}
