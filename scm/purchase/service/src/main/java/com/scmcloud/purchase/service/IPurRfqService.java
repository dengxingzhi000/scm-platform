package com.scmcloud.purchase.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.purchase.domain.entity.PurRfq;

import java.util.List;

public interface IPurRfqService extends IService<PurRfq> {

    PurRfq getByRfqNo(String rfqNo);

    Page<PurRfq> pageQuery(int page, int size, Integer status, Integer rfqType, String keyword);

    List<PurRfq> listByStatus(Integer status);

    boolean publish(String id);

    boolean close(String id);
}
