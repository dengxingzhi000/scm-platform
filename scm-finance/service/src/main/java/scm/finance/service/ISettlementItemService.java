package scm.finance.service;

import scm.finance.domain.entity.SettlementItem;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ISettlementItemService extends IService<SettlementItem> {

    List<SettlementItem> listBySettlementId(String settlementId);
}
