package com.cognora.scm.purchase.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.cognora.scm.purchase.domain.entity.PurRequest;

import java.util.List;

public interface IPurRequestService extends IService<PurRequest> {

    PurRequest getByRequestNo(String requestNo);

    Page<PurRequest> pageQuery(int page, int size, Integer status, Integer requestType, String keyword);

    List<PurRequest> listByStatus(Integer status);
}
