package com.scmcloud.approval.service.flowable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Flowable审批流程服务。
 *
 * <p>提供审批流程的核心功能：
 * <ul>
 *   <li>发起审批流程</li>
 *   <li>审批通过/驳回</li>
 *   <li>查询待办任务</li>
 *   <li>查询审批历史</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalFlowService {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;

    /**
     * 发起审批流程。
     *
     * @param processKey 流程定义key
     * @param businessKey 业务ID
     * @param businessType 业务类型
     * @param applicantId 申请人ID
     * @param variables 流程变量
     * @return 流程实例ID
     */
    @Transactional(rollbackFor = Exception.class)
    public String startProcess(String processKey, String businessKey, String businessType,
                               String applicantId, Map<String, Object> variables) {
        log.info("发起审批流程: processKey={}, businessKey={}, applicantId={}", processKey, businessKey, applicantId);

        Map<String, Object> processVariables = new HashMap<>(variables != null ? variables : Map.of());
        processVariables.put("businessKey", businessKey);
        processVariables.put("businessType", businessType);
        processVariables.put("applicantId", applicantId);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processKey, businessKey, processVariables);

        log.info("审批流程已启动: processInstanceId={}, businessKey={}", processInstance.getId(), businessKey);
        return processInstance.getId();
    }

    /**
     * 审批通过。
     *
     * @param taskId 任务ID
     * @param approverId 审批人ID
     * @param comment 审批意见
     * @param variables 流程变量
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(String taskId, String approverId, String comment, Map<String, Object> variables) {
        log.info("审批通过: taskId={}, approverId={}", taskId, approverId);

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalStateException("任务不存在: " + taskId);
        }

        // 添加审批意见
        if (comment != null && !comment.isBlank()) {
            taskService.addComment(taskId, task.getProcessInstanceId(), "APPROVE", comment);
        }

        // 设置审批人
        taskService.claim(taskId, approverId);

        // 完成任务
        Map<String, Object> taskVariables = new HashMap<>(variables != null ? variables : Map.of());
        taskVariables.put("approved", true);
        taskService.complete(taskId, taskVariables);

        log.info("审批通过完成: taskId={}, processInstanceId={}", taskId, task.getProcessInstanceId());
    }

    /**
     * 审批驳回。
     *
     * @param taskId 任务ID
     * @param approverId 审批人ID
     * @param comment 驳回原因
     */
    @Transactional(rollbackFor = Exception.class)
    public void reject(String taskId, String approverId, String comment) {
        log.info("审批驳回: taskId={}, approverId={}", taskId, approverId);

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalStateException("任务不存在: " + taskId);
        }

        // 添加驳回意见
        if (comment != null && !comment.isBlank()) {
            taskService.addComment(taskId, task.getProcessInstanceId(), "REJECT", comment);
        }

        // 设置审批人
        taskService.claim(taskId, approverId);

        // 完成任务（驳回）
        Map<String, Object> taskVariables = new HashMap<>();
        taskVariables.put("approved", false);
        taskService.complete(taskId, taskVariables);

        log.info("审批驳回完成: taskId={}, processInstanceId={}", taskId, task.getProcessInstanceId());
    }

    /**
     * 查询用户的待办任务。
     *
     * @param assigneeId 用户ID
     * @return 待办任务列表
     */
    public List<TaskDTO> findTodoTasks(String assigneeId) {
        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(assigneeId)
                .orderByTaskCreateTime().desc()
                .list();

        return tasks.stream()
                .map(this::toTaskDTO)
                .collect(Collectors.toList());
    }

    /**
     * 查询流程实例的审批历史。
     *
     * @param processInstanceId 流程实例ID
     * @return 流程实例
     */
    public HistoricProcessInstance getProcessHistory(String processInstanceId) {
        return historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
    }

    /**
     * 查询业务相关的流程实例。
     *
     * @param businessKey 业务ID
     * @return 流程实例
     */
    public ProcessInstance findByBusinessKey(String businessKey) {
        return runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .singleResult();
    }

    private TaskDTO toTaskDTO(Task task) {
        TaskDTO dto = new TaskDTO();
        dto.setTaskId(task.getId());
        dto.setTaskName(task.getName());
        dto.setAssignee(task.getAssignee());
        dto.setProcessInstanceId(task.getProcessInstanceId());
        dto.setCreateTime(task.getCreateTime());
        dto.setDueDate(task.getDueDate());
        dto.setTaskDefinitionKey(task.getTaskDefinitionKey());
        return dto;
    }

    /**
     * 任务DTO。
     */
    @lombok.Data
    public static class TaskDTO {
        private String taskId;
        private String taskName;
        private String assignee;
        private String processInstanceId;
        private java.util.Date createTime;
        private java.util.Date dueDate;
        private String taskDefinitionKey;
    }
}
