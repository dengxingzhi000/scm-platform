package scm.purchase.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scm.purchase.domain.entity.PurReceiptItem;
import scm.purchase.mapper.PurReceiptItemMapper;
import scm.purchase.service.IPurReceiptItemService;

import java.util.List;

@Slf4j
@Service
public class PurReceiptItemServiceImpl extends ServiceImpl<PurReceiptItemMapper, PurReceiptItem> implements IPurReceiptItemService {

    @Override
    public List<PurReceiptItem> listByReceiptId(String receiptId) {
        return lambdaQuery()
                .eq(PurReceiptItem::getReceiptId, receiptId)
                .orderByAsc(PurReceiptItem::getCreateTime)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByReceiptId(String receiptId) {
        return lambdaUpdate()
                .eq(PurReceiptItem::getReceiptId, receiptId)
                .remove();
    }
}
