package com.cognora.platform.common.data.rw.config;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fails startup when read-write routing and {@code @DS} / extra dynamic datasources
 * coexist in the same application.
 * <p>
 * The two mechanisms both try to choose a datasource per call. When {@code @DS}
 * points at a datasource that is not a rw group, that call bypasses the
 * {@link ReadWriteRoutingDataSource} entirely — writes would silently skip the
 * master and {@code @Slave} reads would silently hit a non-replica datasource.
 * Failing at startup is safer than discovering it in production.
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
public class ReadWriteDatasourceConflictChecker {

    private static final String DYNAMIC_DATASOURCE_PREFIX = "spring.datasource.dynamic.datasource.";

    private final ConfigurableApplicationContext context;
    private final Set<String> rwGroups;

    public ReadWriteDatasourceConflictChecker(ConfigurableApplicationContext context, Set<String> rwGroups) {
        this.context = context;
        this.rwGroups = rwGroups;
    }

    public void check() {
        List<String> dsAnnotations = findDsAnnotations();
        // Extra datasource properties only matter when baomidou is actually active;
        // when dynamic.enabled=false they are inert and must not fail startup.
        boolean dynamicActive = context.getEnvironment()
                .getProperty("spring.datasource.dynamic.enabled", Boolean.class, true);
        List<String> extraDatasources = dynamicActive ? findExtraDynamicDatasources() : List.of();
        List<String> primaryFindings = dynamicActive ? checkPrimary() : List.of();

        if (dsAnnotations.isEmpty() && extraDatasources.isEmpty() && primaryFindings.isEmpty()) {
            log.debug("[RW-Config] No @DS / extra dynamic datasource conflict detected");
            return;
        }

        StringBuilder message = new StringBuilder(
                "[RW-Config] Read-write routing cannot coexist with @DS or extra dynamic datasources.\n");
        if (!dsAnnotations.isEmpty()) {
            message.append("  @DS found on:\n");
            dsAnnotations.forEach(finding -> message.append("    - ").append(finding).append('\n'));
        }
        if (!extraDatasources.isEmpty()) {
            message.append("  dynamic datasources not covered by rw groups: ")
                    .append(extraDatasources).append(" (rw groups: ").append(rwGroups).append(")\n");
        }
        primaryFindings.forEach(finding -> message.append("  ").append(finding).append('\n'));
        message.append("  Fix: declare one rw group per @DS name (keep spring.datasource.dynamic.enabled=false), ")
                .append("or remove @DS and the extra datasource configuration, ")
                .append("or disable spring.datasource.rw.enabled.");

        throw new IllegalStateException(message.toString());
    }

    private List<String> checkPrimary() {
        String primary = context.getEnvironment().getProperty("spring.datasource.dynamic.primary");
        if (primary == null || rwGroups.contains(primary)) {
            return List.of();
        }
        return List.of("spring.datasource.dynamic.primary=" + primary
                + " is not a rw group (must be one of " + rwGroups + ")");
    }

    private List<String> findDsAnnotations() {
        List<String> findings = new ArrayList<>();
        for (String beanName : context.getBeanFactory().getBeanDefinitionNames()) {
            Class<?> type;
            try {
                type = context.getBeanFactory().getType(beanName);
            } catch (Exception e) {
                continue;
            }
            if (type == null || !type.getName().startsWith("com.cognora")) {
                continue;
            }

            DS classAnnotation = AnnotationUtils.findAnnotation(type, DS.class);
            if (classAnnotation != null) {
                findings.add(type.getName() + " (class) @DS(\"" + classAnnotation.value() + "\")");
                continue;
            }

            for (Method method : type.getDeclaredMethods()) {
                DS methodAnnotation = AnnotationUtils.findAnnotation(method, DS.class);
                if (methodAnnotation != null) {
                    findings.add(type.getSimpleName() + "." + method.getName()
                            + " @DS(\"" + methodAnnotation.value() + "\")");
                }
            }
        }
        return findings;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<String> findExtraDynamicDatasources() {
        Map<String, Object> configured = Binder.get(context.getEnvironment())
                .bind("spring.datasource.dynamic.datasource", Map.class)
                .orElse(Map.of());

        Set<String> extras = new LinkedHashSet<>();
        configured.keySet().stream()
                .filter(name -> !rwGroups.contains(name))
                .forEach(extras::add);

        if (context.containsBean("dataSource")) {
            try {
                DataSource dataSource = context.getBean(DataSource.class);
                if (dataSource instanceof DynamicRoutingDataSource dynamic) {
                    Map<String, DataSource> current = dynamic.getDataSources();
                    current.keySet().stream()
                            .filter(name -> !rwGroups.contains(name))
                            .forEach(extras::add);
                }
            } catch (Exception e) {
                log.debug("[RW-Config] Could not inspect the primary DataSource: {}", e.getMessage());
            }
        }

        return new ArrayList<>(extras);
    }
}
