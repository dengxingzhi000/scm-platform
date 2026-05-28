package scm.purchase.service;

import com.baomidou.mybatisplus.extension.service.IService;
import scm.purchase.domain.entity.PurPlanItem;

import java.util.List;

public interface IPurPlanItemService extends IService<PurPlanItem> {

    List<PurPlanItem> listByPlanId(String planId);

    boolean deleteByPlanId(String planId);
}
