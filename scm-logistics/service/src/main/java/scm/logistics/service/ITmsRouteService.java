package scm.logistics.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import scm.logistics.domain.entity.TmsRoute;

import java.time.LocalDate;
import java.util.List;

public interface ITmsRouteService extends IService<TmsRoute> {

    Page<TmsRoute> pageList(int page, int size, String courierId, Integer status, LocalDate deliveryDate);

    List<TmsRoute> listByCourierId(String courierId);

    TmsRoute getByRouteNo(String routeNo);
}
