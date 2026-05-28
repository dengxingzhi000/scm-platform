package scm.warehouse.service;

import scm.warehouse.domain.entity.WmsLocation;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IWmsLocationService extends IService<WmsLocation> {

    List<WmsLocation> listByWarehouseId(String warehouseId);

    Page<WmsLocation> pageList(int page, int size, String warehouseId, Integer locationType, Integer status);
}
