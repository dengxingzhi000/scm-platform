package com.scmcloud.warehouse.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.status.StatusValidator;
import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.warehouse.domain.entity.WavePickingStatus;
import com.scmcloud.warehouse.domain.entity.WmsWavePicking;
import com.scmcloud.warehouse.engine.WaveInput;
import com.scmcloud.warehouse.engine.WaveOutput;
import com.scmcloud.warehouse.engine.WavePickingEngine;
import com.scmcloud.warehouse.mapper.WmsWavePickingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WmsWavePickingCommandService {

    private final WmsWavePickingMapper wavePickingMapper;
    private final StatusValidator statusValidator;
    private final WavePickingEngine wavePickingEngine;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public WmsWavePicking create(WmsWavePicking wave) {
        wave.setId(UUIDv7Util.generateString());
        wave.setWaveNo("WAVE" + System.currentTimeMillis());
        wave.setStatus(WavePickingStatus.WAITING.getCode());
        wave.setCreateTime(LocalDateTime.now());
        wave.setUpdateTime(LocalDateTime.now());
        wavePickingMapper.insert(wave);
        log.info("波次拣货单创建成功 id={}, waveNo={}", wave.getId(), wave.getWaveNo());
        return wave;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean update(WmsWavePicking wave) {
        WmsWavePicking existing = wavePickingMapper.selectById(wave.getId());
        if (existing == null) {
            return false;
        }
        if (existing.getStatus() != WavePickingStatus.WAITING.getCode()) {
            throw new IllegalStateException("只有待拣货状态的波次拣货单才能修改");
        }
        wave.setUpdateTime(LocalDateTime.now());
        return wavePickingMapper.updateById(wave) > 0;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(String id) {
        WmsWavePicking wave = wavePickingMapper.selectById(id);
        if (wave == null) {
            return false;
        }
        return wavePickingMapper.deleteById(id) > 0;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean start(String waveId, String pickerId, String pickerName) {
        WmsWavePicking wave = wavePickingMapper.selectById(waveId);
        if (wave == null) {
            log.warn("[start] 波次拣货单不存在: id={}", waveId);
            return false;
        }

        WavePickingStatus currentStatus = WavePickingStatus.fromCode(wave.getStatus());
        statusValidator.validateTransition("WAVE_PICKING", currentStatus.name(), "PICKING");

        wave.setStatus(WavePickingStatus.PICKING.getCode());
        wave.setPickerId(pickerId);
        wave.setPickerName(pickerName);
        wave.setStartedAt(LocalDateTime.now());
        wave.setUpdateTime(LocalDateTime.now());
        wave.setUpdateBy(pickerId);

        boolean success = wavePickingMapper.updateById(wave) > 0;
        if (success) {
            log.info("波次拣货已开始: id={}, waveNo={}, picker={}", waveId, wave.getWaveNo(), pickerName);
        }
        return success;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean complete(String waveId, String operatorId) {
        WmsWavePicking wave = wavePickingMapper.selectById(waveId);
        if (wave == null) {
            log.warn("[complete] 波次拣货单不存在: id={}", waveId);
            return false;
        }

        WavePickingStatus currentStatus = WavePickingStatus.fromCode(wave.getStatus());
        statusValidator.validateTransition("WAVE_PICKING", currentStatus.name(), "COMPLETED");

        wave.setStatus(WavePickingStatus.COMPLETED.getCode());
        wave.setCompletedAt(LocalDateTime.now());
        wave.setUpdateTime(LocalDateTime.now());
        wave.setUpdateBy(operatorId);

        boolean success = wavePickingMapper.updateById(wave) > 0;
        if (success) {
            log.info("波次拣货已完成: id={}, waveNo={}", waveId, wave.getWaveNo());
        }
        return success;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(String waveId, String operatorId) {
        WmsWavePicking wave = wavePickingMapper.selectById(waveId);
        if (wave == null) {
            log.warn("[cancel] 波次拣货单不存在: id={}", waveId);
            return false;
        }

        WavePickingStatus currentStatus = WavePickingStatus.fromCode(wave.getStatus());
        statusValidator.validateTransition("WAVE_PICKING", currentStatus.name(), "CANCELLED");

        wave.setStatus(WavePickingStatus.CANCELLED.getCode());
        wave.setUpdateTime(LocalDateTime.now());
        wave.setUpdateBy(operatorId);

        boolean success = wavePickingMapper.updateById(wave) > 0;
        if (success) {
            log.info("波次拣货已取消: id={}, waveNo={}", waveId, wave.getWaveNo());
        }
        return success;
    }

    public List<WaveOutput.Wave> generateWaves(String warehouseId, List<WaveInput.PendingOrder> orders) {
        log.info("执行波次聚合算法: warehouseId={}, orderCount={}", warehouseId, orders.size());

        WaveInput input = new WaveInput();
        input.setWarehouseId(warehouseId);
        input.setOrders(orders);

        List<WaveOutput.Wave> waves = wavePickingEngine.cluster(input);

        for (WaveOutput.Wave wave : waves) {
            WmsWavePicking waveEntity = new WmsWavePicking();
            waveEntity.setId(wave.getWaveId());
            waveEntity.setWaveNo(wave.getWaveId());
            waveEntity.setWarehouseId(warehouseId);
            waveEntity.setOrderCount(wave.getOrderCount());
            waveEntity.setTotalItems(wave.getTotalSkuCount());
            waveEntity.setStatus(WavePickingStatus.WAITING.getCode());
            waveEntity.setCreateTime(LocalDateTime.now());
            waveEntity.setUpdateTime(LocalDateTime.now());
            wavePickingMapper.insert(waveEntity);
        }

        log.info("波次生成完成: warehouseId={}, waveCount={}", warehouseId, waves.size());
        return waves;
    }
}
