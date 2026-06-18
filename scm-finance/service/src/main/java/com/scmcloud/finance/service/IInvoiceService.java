package com.scmcloud.finance.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.finance.domain.dto.CreateInvoiceRequest;
import com.scmcloud.finance.domain.dto.UpdateInvoiceRequest;
import com.scmcloud.finance.domain.entity.Invoice;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IInvoiceService extends IService<Invoice> {

    Invoice create(CreateInvoiceRequest request);

    Invoice update(String id, UpdateInvoiceRequest request);

    void delete(String id);

    Page<Invoice> pageByPartyId(String partyId, int pageNum, int pageSize);

    Invoice issueInvoice(String id, String issuerName);

    Invoice voidInvoice(String id);

    Invoice redFlushInvoice(String id);
}
