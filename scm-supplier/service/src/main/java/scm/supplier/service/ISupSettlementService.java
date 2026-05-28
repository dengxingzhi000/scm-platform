package scm.supplier.service;

import scm.supplier.domain.entity.SupSettlement;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ISupSettlementService extends IService<SupSettlement> {

    Page<SupSettlement> pageList(int page, int size, String supplierId, Integer status, String settlementPeriod);

    List<SupSettlement> listBySupplierId(String supplierId);

    boolean confirm(String id, String approverId, String approverName);

    boolean markAsPaid(String id, String updateBy);
}
