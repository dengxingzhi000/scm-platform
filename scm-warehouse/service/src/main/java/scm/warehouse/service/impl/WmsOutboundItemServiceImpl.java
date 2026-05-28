package scm.warehouse.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import scm.warehouse.domain.entity.WmsOutboundItem;
import scm.warehouse.mapper.WmsOutboundItemMapper;
import scm.warehouse.service.IWmsOutboundItemService;

import java.util.List;

@Slf4j
@Service
public class WmsOutboundItemServiceImpl extends ServiceImpl<WmsOutboundItemMapper, WmsOutboundItem>
        implements IWmsOutboundItemService {

    @Override
    public List<WmsOutboundItem> listByOutboundId(String outboundId) {
        return lambdaQuery()
                .eq(WmsOutboundItem::getOutboundId, outboundId)
                .eq(WmsOutboundItem::getDeleted, false)
                .list();
    }
}
