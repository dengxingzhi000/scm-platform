package com.scmcloud.purchase.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.purchase.domain.entity.PurQuotation;
import com.scmcloud.purchase.mapper.PurQuotationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scmcloud.common.status.StatusValidator;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurQuotationCommandService {

    private final PurQuotationMapper purQuotationMapper;
    private final StatusValidator statusValidator;

    @Master(reason = "保存报价单")
    @Transactional(rollbackFor = Exception.class)
    public boolean save(PurQuotation entity) {
        return purQuotationMapper.insert(entity) > 0;
    }

    @Master(reason = "更新报价单")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(PurQuotation entity) {
        return purQuotationMapper.updateById(entity) > 0;
    }

    @Master(reason = "删除报价单")
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(String id) {
        return purQuotationMapper.deleteById(id) > 0;
    }

    @Master(reason = "提交报价单")
    @Transactional(rollbackFor = Exception.class)
    public boolean submit(String id) {
        PurQuotation quotation = purQuotationMapper.selectById(id);
        if (quotation == null || quotation.getDeleted()) {
            throw new IllegalArgumentException("报价单不存在: " + id);
        }
        statusValidator.validateTransition("QUOTATION", "DRAFT", "SUBMITTED");
        quotation.setStatus(1); // SUBMITTED
        quotation.setUpdateTime(LocalDateTime.now());
        return purQuotationMapper.updateById(quotation) > 0;
    }

    @Master(reason = "选中报价单")
    @Transactional(rollbackFor = Exception.class)
    public boolean select(String id, String selectedBy, String selectedByName) {
        PurQuotation quotation = purQuotationMapper.selectById(id);
        if (quotation == null || quotation.getDeleted()) {
            throw new IllegalArgumentException("报价单不存在: " + id);
        }
        if (quotation.getStatus() < 1) {
            throw new IllegalStateException("只有已提交的报价单才能被选中");
        }
        quotation.setIsSelected(true);
        quotation.setSelectedBy(selectedBy);
        quotation.setSelectedByName(selectedByName);
        quotation.setSelectedAt(LocalDateTime.now());
        quotation.setUpdateTime(LocalDateTime.now());
        return purQuotationMapper.updateById(quotation) > 0;
    }
}
