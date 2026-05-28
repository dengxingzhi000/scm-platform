package scm.purchase.service;

import com.baomidou.mybatisplus.extension.service.IService;
import scm.purchase.domain.entity.PurRequestItem;

import java.util.List;

public interface IPurRequestItemService extends IService<PurRequestItem> {

    List<PurRequestItem> listByRequestId(String requestId);

    boolean deleteByRequestId(String requestId);
}
