package scm.purchase.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import scm.purchase.domain.entity.PurQuotation;

import java.util.List;

public interface IPurQuotationService extends IService<PurQuotation> {

    PurQuotation getByQuotationNo(String quotationNo);

    Page<PurQuotation> pageQuery(int page, int size, Integer status, String supplierId, String rfqId);

    List<PurQuotation> listByRfqId(String rfqId);

    List<PurQuotation> listBySupplierId(String supplierId);

    boolean submit(String id);

    boolean select(String id, String selectedBy, String selectedByName);
}
