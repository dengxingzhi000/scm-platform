package com.scmcloud.logistics.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.logistics.domain.entity.TmsRoute;
import com.scmcloud.logistics.mapper.TmsRouteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmsRouteCommandService {

    private final TmsRouteMapper tmsRouteMapper;

    @Master(reason = "Save route")
    @Transactional(rollbackFor = Exception.class)
    public boolean save(TmsRoute entity) {
        return tmsRouteMapper.insert(entity) > 0;
    }

    @Master(reason = "Update route")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(TmsRoute entity) {
        return tmsRouteMapper.updateById(entity) > 0;
    }

    @Master(reason = "Delete route")
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(String id) {
        return tmsRouteMapper.deleteById(id) > 0;
    }
}
