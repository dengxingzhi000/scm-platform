package scm.logistics.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import scm.logistics.domain.entity.TmsTracking;

import java.util.List;

public interface ITmsTrackingService extends IService<TmsTracking> {

    List<TmsTracking> listByWaybillId(String waybillId);

    List<TmsTracking> listByWaybillNo(String waybillNo);

    Page<TmsTracking> pageList(int page, int size, String waybillNo, String trackStatus);

    TmsTracking addTracking(TmsTracking tracking);
}
