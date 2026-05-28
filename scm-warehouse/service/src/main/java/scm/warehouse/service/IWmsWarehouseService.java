package scm.warehouse.service;

import scm.warehouse.domain.entity.WmsWarehouse;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IWmsWarehouseService extends IService<WmsWarehouse> {

    List<WmsWarehouse> listEnabled();

    Page<WmsWarehouse> pageList(int page, int size, String warehouseName, Integer warehouseType, Boolean enabled);

    boolean enable(String id);

    boolean disable(String id);
}
