package scm.purchase.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import scm.purchase.domain.entity.PurPlan;

import java.util.List;

public interface IPurPlanService extends IService<PurPlan> {

    PurPlan getByPlanNo(String planNo);

    Page<PurPlan> pageQuery(int page, int size, Integer status, Integer planType, String keyword);

    List<PurPlan> listByStatus(Integer status);

    boolean submit(String id);

    boolean approve(String id, String approverId, String approverName);

    boolean startExecution(String id);

    boolean complete(String id);
}
