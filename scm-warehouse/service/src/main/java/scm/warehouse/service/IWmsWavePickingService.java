package scm.warehouse.service;

import scm.warehouse.domain.entity.WmsWavePicking;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IWmsWavePickingService extends IService<WmsWavePicking> {

    Page<WmsWavePicking> pageList(int page, int size, String warehouseId, Integer status);

    boolean start(String waveId, String pickerId, String pickerName);

    boolean complete(String waveId, String operatorId);

    boolean cancel(String waveId, String operatorId);
}
