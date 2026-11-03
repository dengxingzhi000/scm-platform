package com.cognora.platform.system.service;

import com.cognora.platform.system.domain.entity.SysStatusDict;
import com.cognora.platform.system.domain.entity.SysStatusTransition;

import java.util.List;

public interface ISysStatusDictService {

    List<SysStatusDict> listStatusDict(String bizType);

    SysStatusDict getStatusByCode(String bizType, String statusCode);

    void createStatusDict(SysStatusDict entity);

    void updateStatusDict(SysStatusDict entity);

    void deleteStatusDict(String id);

    List<SysStatusTransition> listTransitions(String bizType);

    List<SysStatusTransition> listTransitionsFrom(String bizType, String fromStatus);

    boolean canTransition(String bizType, String fromStatus, String toStatus);

    SysStatusTransition findTransition(String bizType, String fromStatus, String toStatus, String actionCode);

    void createTransition(SysStatusTransition entity);

    void updateTransition(SysStatusTransition entity);

    void deleteTransition(String id);
}
