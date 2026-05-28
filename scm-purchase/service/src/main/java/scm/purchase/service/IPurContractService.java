package scm.purchase.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import scm.purchase.domain.entity.PurContract;

import java.util.List;

public interface IPurContractService extends IService<PurContract> {

    PurContract getByContractNo(String contractNo);

    Page<PurContract> pageQuery(int page, int size, Integer status, Integer contractType, String supplierId, String keyword);

    List<PurContract> listByStatus(Integer status);

    List<PurContract> listBySupplierId(String supplierId);

    boolean sign(String id, String signedBy, String signedByName);

    boolean terminate(String id);
}
