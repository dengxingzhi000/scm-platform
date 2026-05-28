package scm.finance.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import scm.finance.domain.entity.SettlementOrder;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;

public interface ISettlementOrderService extends IService<SettlementOrder> {

    SettlementOrder createSettlement(SettlementOrder order);

    SettlementOrder confirmSettlement(String id, String approverId, String approverName);

    SettlementOrder recordPayment(String id, BigDecimal amount);

    Page<SettlementOrder> listByStatus(Integer status, int page, int size);
}
