package scm.purchase.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import scm.purchase.domain.entity.PurReceipt;

import java.util.List;

public interface IPurReceiptService extends IService<PurReceipt> {

    PurReceipt getByReceiptNo(String receiptNo);

    Page<PurReceipt> pageQuery(int page, int size, Integer status, Integer receiptType, String supplierId, String keyword);

    List<PurReceipt> listByOrderId(String orderId);

    boolean receive(String id, String receiverId, String receiverName);

    boolean qualityInspect(String id, String inspectorId, String inspectorName, Integer result, String remark);

    boolean shelve(String id, String shelvedBy, String shelvedByName);
}
