package com.scmcloud.system.service;

import com.scmcloud.system.domain.entity.SysDictItem;
import com.scmcloud.system.domain.entity.SysDictType;

import java.util.List;

public interface ISysDictService {

    List<SysDictType> listDictTypes(String dictCode, Integer status);

    SysDictType getDictTypeById(String id);

    SysDictType getDictTypeByCode(String dictCode);

    void createDictType(SysDictType entity);

    void updateDictType(SysDictType entity);

    void deleteDictType(String id);

    List<SysDictItem> listDictItems(String dictCode);

    SysDictItem getDictItemById(String id);

    void createDictItem(SysDictItem entity);

    void updateDictItem(SysDictItem entity);

    void deleteDictItem(String id);
}
