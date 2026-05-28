package scm.warehouse.service;

import scm.warehouse.domain.entity.WmsInboundItem;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IWmsInboundItemService extends IService<WmsInboundItem> {

    List<WmsInboundItem> listByInboundId(String inboundId);
}
