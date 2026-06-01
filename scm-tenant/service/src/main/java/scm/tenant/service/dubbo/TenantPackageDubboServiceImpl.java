package scm.tenant.service.dubbo;

import com.frog.tenant.api.command.TenantSubscribeCommand;
import com.frog.tenant.api.dto.subscription.TenantPackageDTO;
import com.frog.tenant.api.dto.subscription.TenantSubscriptionDTO;
import com.frog.tenant.api.service.TenantPackageDubboService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;
import scm.tenant.domain.entity.TenantPackage;
import scm.tenant.domain.entity.TenantSubscription;
import scm.tenant.service.ITenantPackageService;
import scm.tenant.service.ITenantSubscriptionService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@DubboService
@Component
@RequiredArgsConstructor
public class TenantPackageDubboServiceImpl implements TenantPackageDubboService {

    private final ITenantPackageService packageService;
    private final ITenantSubscriptionService subscriptionService;

    @Override
    public TenantPackageDTO getPackageById(String packageId) {
        log.debug("Dubbo查询套餐: packageId={}", packageId);
        TenantPackage pkg = packageService.lambdaQuery()
                .eq(TenantPackage::getId, packageId)
                .eq(TenantPackage::getDeleted, false)
                .one();
        return pkg == null ? null : convertPackageToDTO(pkg);
    }

    @Override
    public TenantPackageDTO getTenantCurrentPackage(String tenantId) {
        log.debug("Dubbo查询租户当前套餐: tenantId={}", tenantId);
        TenantSubscription subscription = subscriptionService.lambdaQuery()
                .eq(TenantSubscription::getTenantId, tenantId)
                .eq(TenantSubscription::getStatus, 1)
                .orderByDesc(TenantSubscription::getCreateTime)
                .last("LIMIT 1")
                .one();

        if (subscription == null) {
            return null;
        }

        return getPackageById(subscription.getPackageId());
    }

    @Override
    public List<TenantPackageDTO> listAvailablePackages() {
        log.debug("Dubbo查询可用套餐列表");
        List<TenantPackage> packages = packageService.lambdaQuery()
                .eq(TenantPackage::getEnabled, true)
                .eq(TenantPackage::getDeleted, false)
                .orderByAsc(TenantPackage::getSortOrder)
                .list();
        return packages.stream().map(this::convertPackageToDTO).toList();
    }

    @Override
    public TenantSubscriptionDTO getActiveSubscription(String tenantId) {
        log.debug("Dubbo查询活跃订阅: tenantId={}", tenantId);
        TenantSubscription subscription = subscriptionService.lambdaQuery()
                .eq(TenantSubscription::getTenantId, tenantId)
                .eq(TenantSubscription::getStatus, 1)
                .orderByDesc(TenantSubscription::getCreateTime)
                .last("LIMIT 1")
                .one();
        return subscription == null ? null : convertSubscriptionToDTO(subscription);
    }

    @Override
    public String subscribe(String tenantId, TenantSubscribeCommand command) {
        log.info("Dubbo订阅: tenantId={}, packageId={}", tenantId, command.getPackageId());

        TenantSubscription subscription = new TenantSubscription();
        subscription.setId(UUID.randomUUID().toString());
        subscription.setTenantId(tenantId);
        subscription.setPackageId(command.getPackageId());
        subscription.setSubscriptionType(command.getSubscriptionType());
        subscription.setAutoRenew(command.getAutoRenew());
        subscription.setStatus(0);
        subscription.setCreateTime(LocalDateTime.now());
        subscription.setUpdateTime(LocalDateTime.now());

        subscriptionService.save(subscription);
        log.info("Dubbo订阅成功: id={}", subscription.getId());
        return subscription.getId();
    }

    private TenantPackageDTO convertPackageToDTO(TenantPackage pkg) {
        TenantPackageDTO dto = new TenantPackageDTO();
        dto.setId(pkg.getId());
        dto.setPackageCode(pkg.getPackageCode());
        dto.setPackageName(pkg.getPackageName());
        dto.setPackageLevel(pkg.getPackageLevel());
        dto.setPriceMonthly(pkg.getPriceMonthly());
        dto.setPriceYearly(pkg.getPriceYearly());
        dto.setMaxUsers(pkg.getMaxUsers());
        dto.setMaxWarehouses(pkg.getMaxWarehouses());
        dto.setMaxSkus(pkg.getMaxSkus());
        dto.setMaxOrdersPerDay(pkg.getMaxOrdersPerDay());
        dto.setFeatures(pkg.getFeatures());
        dto.setEnabled(pkg.getEnabled());
        return dto;
    }

    private TenantSubscriptionDTO convertSubscriptionToDTO(TenantSubscription sub) {
        TenantSubscriptionDTO dto = new TenantSubscriptionDTO();
        dto.setId(sub.getId());
        dto.setTenantId(sub.getTenantId());
        dto.setPackageId(sub.getPackageId());
        dto.setSubscriptionType(sub.getSubscriptionType());
        dto.setStatus(sub.getStatus());
        dto.setStartDate(sub.getStartDate());
        dto.setEndDate(sub.getEndDate());
        dto.setAutoRenew(sub.getAutoRenew());
        dto.setActualPrice(sub.getActualPrice());
        dto.setPaymentStatus(sub.getPaymentStatus());
        dto.setCreateTime(sub.getCreateTime());
        return dto;
    }
}
