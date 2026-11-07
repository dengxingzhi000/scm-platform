package com.cognora.scm.finance.config;

import com.cognora.scm.finance.domain.enums.CloseDateBasis;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 结账口径配置，默认值即业务方当前可接受的默认实现；
 * 口径说明见 docs/guides/FINANCE_PERIOD_CLOSE_RULES.md。
 */
@Data
@Component
@ConfigurationProperties(prefix = "finance.close")
public class CloseCriteriaProperties {

    /** 归属期日期口径，默认按业务日期。 */
    private CloseDateBasis dateBasis = CloseDateBasis.BUSINESS_DATE;

    /** 是否把「已发货未开票」的结算明细纳入本期，默认纳入。 */
    private boolean includeShippedUnbilled = true;

    /** 是否排除已驳回/已作废单据（发票状态 3），默认排除；红冲（状态 4）始终排除。 */
    private boolean excludeRejected = true;

    /** 视为「已发货」的结算明细单据类型；配置空则按不识别处理。 */
    private List<String> shippedUnbilledDocumentTypes =
            new ArrayList<>(List.of("SHIPMENT", "OUTBOUND", "SALE_OUTBOUND"));
}
