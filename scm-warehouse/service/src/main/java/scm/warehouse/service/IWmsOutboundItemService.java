package scm.warehouse.service;

import scm.warehouse.domain.entity.WmsOutboundItem;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IWmsOutboundItemService extends IService<WmsOutboundItem> {

    List<WmsOutboundItem> listByOutboundId(String outboundId);
}
