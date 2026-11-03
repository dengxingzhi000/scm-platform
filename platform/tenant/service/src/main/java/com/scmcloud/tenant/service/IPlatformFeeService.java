package com.scmcloud.tenant.service;

import java.time.YearMonth;
import java.util.UUID;

public interface IPlatformFeeService {
    int calculateMonthlyFees(YearMonth targetMonth, UUID tenantId);
}
