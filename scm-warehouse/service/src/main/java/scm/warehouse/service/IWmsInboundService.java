package scm.warehouse.service;

import scm.warehouse.domain.entity.WmsInbound;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IWmsInboundService extends IService<WmsInbound> {

    Page<WmsInbound> pageList(int page, int size, String warehouseId, Integer inboundType, Integer status);

    boolean receive(String inboundId, String operatorId, String operatorName);

    boolean cancel(String inboundId, String operatorId, String operatorName);
}
