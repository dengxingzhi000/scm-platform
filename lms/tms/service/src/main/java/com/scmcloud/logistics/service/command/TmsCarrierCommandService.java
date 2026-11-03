package com.scmcloud.logistics.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.logistics.domain.entity.TmsCarrier;
import com.scmcloud.logistics.mapper.TmsCarrierMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmsCarrierCommandService {

    private final TmsCarrierMapper tmsCarrierMapper;

    @Master(reason = "Save carrier")
    @Transactional(rollbackFor = Exception.class)
    public boolean save(TmsCarrier entity) {
        return tmsCarrierMapper.insert(entity) > 0;
    }

    @Master(reason = "Update carrier")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(TmsCarrier entity) {
        return tmsCarrierMapper.updateById(entity) > 0;
    }

    @Master(reason = "Delete carrier")
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(String id) {
        return tmsCarrierMapper.deleteById(id) > 0;
    }
}
