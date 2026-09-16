package com.scmcloud.purchase.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.purchase.domain.entity.PurRequest;

import java.util.List;

public interface IPurRequestService extends IService<PurRequest> {

    PurRequest getByRequestNo(String requestNo);

    Page<PurRequest> pageQuery(int page, int size, Integer status, Integer requestType, String keyword);

    List<PurRequest> listByStatus(Integer status);

    boolean submit(String id);

    boolean approve(String id, String approverId, String approverName);

    boolean reject(String id, String approverId, String approverName, String reason);

    boolean close(String id);

    boolean convertToOrder(String id, String orderId, String orderNo);
}
