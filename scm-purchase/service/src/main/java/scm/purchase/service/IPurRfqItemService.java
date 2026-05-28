package scm.purchase.service;

import com.baomidou.mybatisplus.extension.service.IService;
import scm.purchase.domain.entity.PurRfqItem;

import java.util.List;

public interface IPurRfqItemService extends IService<PurRfqItem> {

    List<PurRfqItem> listByRfqId(String rfqId);

    boolean deleteByRfqId(String rfqId);
}
