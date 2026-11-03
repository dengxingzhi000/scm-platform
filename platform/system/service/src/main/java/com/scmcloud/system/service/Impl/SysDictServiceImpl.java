package com.scmcloud.system.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scmcloud.system.domain.entity.SysDictItem;
import com.scmcloud.system.domain.entity.SysDictType;
import com.scmcloud.system.mapper.SysDictItemMapper;
import com.scmcloud.system.mapper.SysDictTypeMapper;
import com.scmcloud.system.service.ISysDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SysDictServiceImpl implements ISysDictService {

    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictItemMapper dictItemMapper;

    @Override
    public List<SysDictType> listDictTypes(String dictCode, Integer status) {
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getDeleted, false)
                .eq(dictCode != null, SysDictType::getDictCode, dictCode)
                .eq(status != null, SysDictType::getStatus, status)
                .orderByAsc(SysDictType::getSortOrder);
        return dictTypeMapper.selectList(wrapper);
    }

    @Override
    public SysDictType getDictTypeById(String id) {
        return dictTypeMapper.selectById(id);
    }

    @Override
    public SysDictType getDictTypeByCode(String dictCode) {
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getDictCode, dictCode)
                .eq(SysDictType::getDeleted, false);
        return dictTypeMapper.selectOne(wrapper);
    }

    @Override
    public void createDictType(SysDictType entity) {
        entity.setId(UUID.randomUUID().toString());
        dictTypeMapper.insert(entity);
    }

    @Override
    public void updateDictType(SysDictType entity) {
        dictTypeMapper.updateById(entity);
    }

    @Override
    public void deleteDictType(String id) {
        dictTypeMapper.deleteById(id);
    }

    @Override
    public List<SysDictItem> listDictItems(String dictCode) {
        LambdaQueryWrapper<SysDictItem> wrapper = new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getDictCode, dictCode)
                .eq(SysDictItem::getDeleted, false)
                .orderByAsc(SysDictItem::getSortOrder);
        return dictItemMapper.selectList(wrapper);
    }

    @Override
    public SysDictItem getDictItemById(String id) {
        return dictItemMapper.selectById(id);
    }

    @Override
    public void createDictItem(SysDictItem entity) {
        entity.setId(UUID.randomUUID().toString());
        dictItemMapper.insert(entity);
    }

    @Override
    public void updateDictItem(SysDictItem entity) {
        dictItemMapper.updateById(entity);
    }

    @Override
    public void deleteDictItem(String id) {
        dictItemMapper.deleteById(id);
    }
}
