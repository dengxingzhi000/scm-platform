package scm.tenant.service.dubbo;

import com.frog.tenant.api.command.TenantConfigUpdateCommand;
import com.frog.tenant.api.dto.config.TenantConfigDTO;
import com.frog.tenant.api.service.TenantConfigDubboService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;
import scm.tenant.domain.entity.TenantConfig;
import scm.tenant.service.ITenantConfigService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@DubboService
@Component
@RequiredArgsConstructor
public class TenantConfigDubboServiceImpl implements TenantConfigDubboService {

    private final ITenantConfigService configService;

    @Override
    public List<TenantConfigDTO> listConfigs(String tenantId) {
        log.debug("Dubbo查询租户配置列表: tenantId={}", tenantId);
        List<TenantConfig> configs = configService.lambdaQuery()
                .eq(TenantConfig::getTenantId, tenantId)
                .orderByAsc(TenantConfig::getConfigCategory)
                .list();
        return configs.stream().map(this::convertToDTO).toList();
    }

    @Override
    public String getConfigValue(String tenantId, String configKey) {
        log.debug("Dubbo查询配置值: tenantId={}, configKey={}", tenantId, configKey);
        TenantConfig config = configService.lambdaQuery()
                .eq(TenantConfig::getTenantId, tenantId)
                .eq(TenantConfig::getConfigKey, configKey)
                .one();
        return config == null ? null : config.getConfigValue();
    }

    @Override
    public void updateConfig(String tenantId, TenantConfigUpdateCommand command) {
        log.info("Dubbo更新租户配置: tenantId={}, configKey={}", tenantId, command.getConfigKey());

        TenantConfig existing = configService.lambdaQuery()
                .eq(TenantConfig::getTenantId, tenantId)
                .eq(TenantConfig::getConfigKey, command.getConfigKey())
                .one();

        if (existing != null) {
            existing.setConfigValue(command.getConfigValue());
            existing.setUpdateTime(LocalDateTime.now());
            configService.updateById(existing);
        } else {
            TenantConfig newConfig = new TenantConfig();
            newConfig.setId(UUID.randomUUID().toString());
            newConfig.setTenantId(tenantId);
            newConfig.setConfigKey(command.getConfigKey());
            newConfig.setConfigValue(command.getConfigValue());
            newConfig.setCreateTime(LocalDateTime.now());
            newConfig.setUpdateTime(LocalDateTime.now());
            configService.save(newConfig);
        }
    }

    @Override
    public Map<String, String> getFeatureFlags(String tenantId) {
        log.debug("Dubbo查询功能开关: tenantId={}", tenantId);
        List<TenantConfig> configs = configService.lambdaQuery()
                .eq(TenantConfig::getTenantId, tenantId)
                .eq(TenantConfig::getConfigCategory, "FEATURE")
                .list();

        Map<String, String> flags = new HashMap<>();
        for (TenantConfig config : configs) {
            flags.put(config.getConfigKey(), config.getConfigValue());
        }
        return flags;
    }

    private TenantConfigDTO convertToDTO(TenantConfig config) {
        TenantConfigDTO dto = new TenantConfigDTO();
        dto.setId(config.getId());
        dto.setTenantId(config.getTenantId());
        dto.setConfigCategory(config.getConfigCategory());
        dto.setConfigKey(config.getConfigKey());
        dto.setConfigValue(config.getConfigValue());
        dto.setValueType(config.getValueType());
        dto.setDescription(config.getDescription());
        return dto;
    }
}
