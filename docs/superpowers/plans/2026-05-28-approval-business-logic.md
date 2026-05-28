# Approval Business Logic Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the approval workflow logic for `scm-approval` module with submit/approve/reject/list operations.

**Architecture:** Service layer implements CRUD + approval state machine (pending → approving → approved/rejected). Controller exposes REST endpoints using `ApiResponse` wrapper.

**Tech Stack:** Spring Boot, MyBatis-Plus, Lombok, PostgreSQL

---

## File Structure

| File | Action | Purpose |
|------|--------|---------|
| `scm-approval/service/src/main/java/scm/approval/service/ISysPermissionApprovalService.java` | Modify | Add method signatures |
| `scm-approval/service/src/main/java/scm/approval/service/impl/SysPermissionApprovalServiceImpl.java` | Modify | Implement approval workflow |
| `scm-approval/service/src/main/java/scm/approval/controller/SysPermissionApprovalController.java` | Modify | REST endpoints |

---

### Task 1: Add Service Interface Methods

**Files:**
- Modify: `scm-approval/service/src/main/java/scm/approval/service/ISysPermissionApprovalService.java`

- [ ] **Step 1: Add method signatures to interface**

```java
package scm.approval.service;

import scm.approval.domain.entity.SysPermissionApproval;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface ISysPermissionApprovalService extends IService<SysPermissionApproval> {

    SysPermissionApproval submitApproval(SysPermissionApproval approval);

    SysPermissionApproval approve(String approvalId, String approverId, String approverName);

    SysPermissionApproval reject(String approvalId, String approverId, String approverName, String rejectReason);

    List<SysPermissionApproval> listByApplicant(String applicantId);

    List<SysPermissionApproval> listPending(String approverId);
}
```

- [ ] **Step 2: Verify compilation**

Run: `mvn compile -pl scm-approval/service -am -f com.scm.parent/pom.xml`
Expected: BUILD SUCCESS

---

### Task 2: Implement Service Methods

**Files:**
- Modify: `scm-approval/service/src/main/java/scm/approval/service/impl/SysPermissionApprovalServiceImpl.java`

- [ ] **Step 1: Implement full service class**

```java
package scm.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.frog.common.util.UUIDv7Util;
import scm.approval.domain.entity.SysPermissionApproval;
import scm.approval.mapper.SysPermissionApprovalMapper;
import scm.approval.service.ISysPermissionApprovalService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class SysPermissionApprovalServiceImpl extends ServiceImpl<SysPermissionApprovalMapper, SysPermissionApproval>
        implements ISysPermissionApprovalService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVING = 1;
    private static final int STATUS_APPROVED = 2;
    private static final int STATUS_REJECTED = 3;
    private static final int STATUS_WITHDRAWN = 4;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysPermissionApproval submitApproval(SysPermissionApproval approval) {
        log.info("提交审批申请: applicantId={}, type={}", approval.getApplicantId(), approval.getApprovalType());

        approval.setId(UUIDv7Util.generateUUID());
        approval.setApprovalStatus(STATUS_PENDING);
        approval.setCreateTime(LocalDateTime.now());
        approval.setUpdateTime(LocalDateTime.now());

        boolean success = save(approval);
        if (!success) {
            throw new RuntimeException("提交审批申请失败");
        }

        log.info("审批申请提交成功: id={}", approval.getId());
        return approval;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysPermissionApproval approve(String approvalId, String approverId, String approverName) {
        log.info("审批通过: approvalId={}, approverId={}", approvalId, approverId);

        SysPermissionApproval approval = getById(approvalId);
        if (approval == null) {
            throw new IllegalArgumentException("审批记录不存在: " + approvalId);
        }

        if (approval.getApprovalStatus() != STATUS_PENDING
                && approval.getApprovalStatus() != STATUS_APPROVING) {
            throw new IllegalStateException("当前状态不允许审批: status=" + approval.getApprovalStatus());
        }

        approval.setApprovalStatus(STATUS_APPROVED);
        approval.setApprovedBy(approverId);
        approval.setApproverName(approverName);
        approval.setApprovedTime(LocalDateTime.now());
        approval.setUpdateTime(LocalDateTime.now());

        boolean success = updateById(approval);
        if (!success) {
            throw new RuntimeException("审批操作失败");
        }

        log.info("审批通过成功: id={}", approvalId);
        return approval;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysPermissionApproval reject(String approvalId, String approverId, String approverName, String rejectReason) {
        log.info("审批拒绝: approvalId={}, approverId={}", approvalId, approverId);

        SysPermissionApproval approval = getById(approvalId);
        if (approval == null) {
            throw new IllegalArgumentException("审批记录不存在: " + approvalId);
        }

        if (approval.getApprovalStatus() != STATUS_PENDING
                && approval.getApprovalStatus() != STATUS_APPROVING) {
            throw new IllegalStateException("当前状态不允许审批: status=" + approval.getApprovalStatus());
        }

        approval.setApprovalStatus(STATUS_REJECTED);
        approval.setApprovedBy(approverId);
        approval.setApproverName(approverName);
        approval.setApprovedTime(LocalDateTime.now());
        approval.setRejectReason(rejectReason);
        approval.setUpdateTime(LocalDateTime.now());

        boolean success = updateById(approval);
        if (!success) {
            throw new RuntimeException("审批拒绝操作失败");
        }

        log.info("审批拒绝成功: id={}", approvalId);
        return approval;
    }

    @Override
    public List<SysPermissionApproval> listByApplicant(String applicantId) {
        log.debug("查询申请人审批列表: applicantId={}", applicantId);

        LambdaQueryWrapper<SysPermissionApproval> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysPermissionApproval::getApplicantId, applicantId)
               .orderByDesc(SysPermissionApproval::getCreateTime);

        return list(wrapper);
    }

    @Override
    public List<SysPermissionApproval> listPending(String approverId) {
        log.debug("查询待审批列表: approverId={}", approverId);

        LambdaQueryWrapper<SysPermissionApproval> wrapper = Wrappers.lambdaQuery();
        wrapper.and(w -> w.eq(SysPermissionApproval::getApprovalStatus, STATUS_PENDING)
                          .or()
                          .eq(SysPermissionApproval::getApprovalStatus, STATUS_APPROVING));
        wrapper.orderByDesc(SysPermissionApproval::getCreateTime);

        return list(wrapper);
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `mvn compile -pl scm-approval/service -am -f com.scm.parent/pom.xml`
Expected: BUILD SUCCESS

---

### Task 3: Implement Controller Endpoints

**Files:**
- Modify: `scm-approval/service/src/main/java/scm/approval/controller/SysPermissionApprovalController.java`

- [ ] **Step 1: Implement REST controller**

```java
package scm.approval.controller;

import com.frog.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import scm.approval.domain.entity.SysPermissionApproval;
import scm.approval.service.ISysPermissionApprovalService;

import java.util.List;

@RestController
@RequestMapping("/sys-permission-approval")
@RequiredArgsConstructor
public class SysPermissionApprovalController {

    private final ISysPermissionApprovalService approvalService;

    @PostMapping("/submit")
    public ApiResponse<SysPermissionApproval> submitApproval(@RequestBody SysPermissionApproval approval) {
        SysPermissionApproval result = approvalService.submitApproval(approval);
        return ApiResponse.success(result);
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<SysPermissionApproval> approve(
            @PathVariable String id,
            @RequestParam String approverId,
            @RequestParam String approverName) {
        SysPermissionApproval result = approvalService.approve(id, approverId, approverName);
        return ApiResponse.success(result);
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<SysPermissionApproval> reject(
            @PathVariable String id,
            @RequestParam String approverId,
            @RequestParam String approverName,
            @RequestParam String rejectReason) {
        SysPermissionApproval result = approvalService.reject(id, approverId, approverName, rejectReason);
        return ApiResponse.success(result);
    }

    @GetMapping("/applicant/{applicantId}")
    public ApiResponse<List<SysPermissionApproval>> listByApplicant(@PathVariable String applicantId) {
        List<SysPermissionApproval> result = approvalService.listByApplicant(applicantId);
        return ApiResponse.success(result);
    }

    @GetMapping("/pending")
    public ApiResponse<List<SysPermissionApproval>> listPending(@RequestParam String approverId) {
        List<SysPermissionApproval> result = approvalService.listPending(approverId);
        return ApiResponse.success(result);
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `mvn compile -pl scm-approval/service -am -f com.scm.parent/pom.xml`
Expected: BUILD SUCCESS

- [ ] **Step 3: Final build verification**

Run: `mvn clean install -DskipTests -pl scm-approval/service -am -f com.scm.parent/pom.xml`
Expected: BUILD SUCCESS
