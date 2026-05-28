package scm.purchase.service;

import com.baomidou.mybatisplus.extension.service.IService;
import scm.purchase.domain.entity.PurReceiptItem;

import java.util.List;

public interface IPurReceiptItemService extends IService<PurReceiptItem> {

    List<PurReceiptItem> listByReceiptId(String receiptId);

    boolean deleteByReceiptId(String receiptId);
}
