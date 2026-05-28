package scm.finance.service;

import scm.finance.domain.entity.Invoice;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IInvoiceService extends IService<Invoice> {

    List<Invoice> listByPartyId(String partyId);

    Invoice issueInvoice(String id, String issuerName);

    Invoice voidInvoice(String id);

    Invoice redFlushInvoice(String id);
}
