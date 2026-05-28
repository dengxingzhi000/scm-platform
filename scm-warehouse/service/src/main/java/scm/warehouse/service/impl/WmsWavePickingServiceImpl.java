package scm.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import scm.warehouse.domain.entity.WmsWavePicking;
import scm.warehouse.mapper.WmsWavePickingMapper;
import scm.warehouse.service.IWmsWavePickingService;

import java.time.LocalDateTime;

@Slf4j
@Service
public class WmsWavePickingServiceImpl extends ServiceImpl<WmsWavePickingMapper, WmsWavePicking>
        implements IWmsWavePickingService {

    @Override
    public Page<WmsWavePicking> pageList(int page, int size, String warehouseId, Integer status) {
        LambdaQueryWrapper<WmsWavePicking> wrapper = Wrappers.lambdaQuery();

        if (StringUtils.hasText(warehouseId)) {
            wrapper.eq(WmsWavePicking::getWarehouseId, warehouseId);
        }
        if (status != null) {
            wrapper.eq(WmsWavePicking::getStatus, status);
        }
        wrapper.orderByDesc(WmsWavePicking::getCreateTime);

        return page(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean start(String waveId, String pickerId, String pickerName) {
        WmsWavePicking wave = getById(waveId);
        if (wave == null) {
            log.warn("波次拣货单不存在: id={}", waveId);
            return false;
        }
        if (wave.getStatus() != 0) {
            throw new IllegalStateException("波次拣货单状态不允许开始拣货，当前状态: " + wave.getStatus());
        }

        wave.setStatus(1); // 1-拣货中
        wave.setPickerId(pickerId);
        wave.setPickerName(pickerName);
        wave.setStartedAt(LocalDateTime.now());
        wave.setUpdateTime(LocalDateTime.now());
        wave.setUpdateBy(pickerId);

        boolean success = updateById(wave);
        if (success) {
            log.info("波次拣货已开始: id={}, waveNo={}, picker={}", waveId, wave.getWaveNo(), pickerName);
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean complete(String waveId, String operatorId) {
        WmsWavePicking wave = getById(waveId);
        if (wave == null) {
            log.warn("波次拣货单不存在: id={}", waveId);
            return false;
        }
        if (wave.getStatus() != 1) {
            throw new IllegalStateException("波次拣货单状态不允许完成，当前状态: " + wave.getStatus());
        }

        wave.setStatus(2); // 2-已完成
        wave.setCompletedAt(LocalDateTime.now());
        wave.setUpdateTime(LocalDateTime.now());
        wave.setUpdateBy(operatorId);

        boolean success = updateById(wave);
        if (success) {
            log.info("波次拣货已完成: id={}, waveNo={}", waveId, wave.getWaveNo());
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(String waveId, String operatorId) {
        WmsWavePicking wave = getById(waveId);
        if (wave == null) {
            log.warn("波次拣货单不存在: id={}", waveId);
            return false;
        }
        if (wave.getStatus() == 2) {
            throw new IllegalStateException("已完成的波次拣货单不能取消");
        }

        wave.setStatus(3); // 3-已取消
        wave.setUpdateTime(LocalDateTime.now());
        wave.setUpdateBy(operatorId);

        boolean success = updateById(wave);
        if (success) {
            log.info("波次拣货已取消: id={}, waveNo={}", waveId, wave.getWaveNo());
        }
        return success;
    }
}
