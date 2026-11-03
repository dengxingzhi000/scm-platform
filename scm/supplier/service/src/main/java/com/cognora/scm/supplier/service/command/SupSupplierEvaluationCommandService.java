package com.cognora.scm.supplier.service.command;

import com.cognora.platform.common.data.rw.annotation.Master;
import com.cognora.scm.supplier.domain.entity.SupSupplierEvaluation;
import com.cognora.scm.supplier.mapper.SupSupplierEvaluationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupSupplierEvaluationCommandService {

    private final SupSupplierEvaluationMapper supSupplierEvaluationMapper;

    @Master(reason = "保存供应商评价")
    @Transactional(rollbackFor = Exception.class)
    public boolean save(SupSupplierEvaluation entity) {
        return supSupplierEvaluationMapper.insert(entity) > 0;
    }

    @Master(reason = "更新供应商评价")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(SupSupplierEvaluation entity) {
        return supSupplierEvaluationMapper.updateById(entity) > 0;
    }

    @Master(reason = "删除供应商评价")
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(String id) {
        return supSupplierEvaluationMapper.deleteById(id) > 0;
    }
}
