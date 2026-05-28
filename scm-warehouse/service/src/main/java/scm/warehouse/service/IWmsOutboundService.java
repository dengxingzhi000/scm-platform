package scm.warehouse.service;

import scm.warehouse.domain.entity.WmsOutbound;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IWmsOutboundService extends IService<WmsOutbound> {

    Page<WmsOutbound> pageList(int page, int size, String warehouseId, Integer outboundType, Integer status);

    boolean ship(String outboundId, String operatorId, String operatorName);

    boolean cancel(String outboundId, String operatorId, String operatorName);
}
