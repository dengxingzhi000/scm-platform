package com.cognora.platform.system.service.support;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.dto.user.CreateUserRequest;
import com.cognora.platform.common.dto.user.UserImportResult;
import com.cognora.platform.common.dto.user.UserQueryRequest;
import com.cognora.platform.common.dto.user.UserSummaryDTO;
import com.cognora.platform.common.exception.BusinessException;
import com.cognora.platform.common.response.ResultCode;
import com.cognora.platform.common.security.PermissionChecker;
import com.cognora.platform.common.tenant.TenantValidationUtil;
import com.cognora.platform.common.web.enums.SensitiveType;
import com.cognora.platform.common.web.util.SecurityUtils;
import com.cognora.platform.system.mapper.SysUserMapper;
import com.cognora.platform.system.service.command.ISysUserCommandService;
import com.cognora.platform.system.service.query.ISysUserQueryService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 用户 CSV 导入/导出。不引入 Excel 重依赖，模板与导出均为 UTF-8 BOM CSV。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserImportExportService {

    private static final int MAX_FILE_BYTES = 5 * 1024 * 1024;
    private static final int MAX_ROWS = 500;
    private static final int MAX_EXPORT_ROWS = 10_000;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static final String TEMPLATE_HEADER =
            "username,password,realName,email,phone,deptId,remark\n";
    private static final String EXPORT_HEADER =
            "username,realName,phone,email,status,deptName,roleNames,createTime,lastLoginTime,remark\n";

    private final ISysUserCommandService userCommandService;
    private final ISysUserQueryService userQueryService;
    private final SysUserMapper userMapper;
    private final PasswordPolicyService passwordPolicyService;
    private final PermissionChecker permissionChecker;

    public String template() {
        return "\uFEFF" + TEMPLATE_HEADER
                + "zhangsan,Abcd1234!,张三,zhangsan@example.com,13800000000,,\n";
    }

    public UserImportResult importUsers(MultipartFile file, boolean dryRun) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATION_FAILED.getCode(), "导入文件不能为空");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BusinessException(ResultCode.VALIDATION_FAILED.getCode(), "导入文件不能超过5MB");
        }

        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.VALIDATION_FAILED.getCode(), "读取导入文件失败");
        }
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }

        List<List<String>> rows = CsvUtils.parse(content);
        if (rows.isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATION_FAILED.getCode(), "导入文件没有内容");
        }

        Map<String, Integer> header = headerIndex(rows.get(0));
        List<List<String>> dataRows = rows.subList(1, rows.size());
        if (dataRows.size() > MAX_ROWS) {
            throw new BusinessException(ResultCode.VALIDATION_FAILED.getCode(), "单次最多导入500行");
        }

        UUID tenantId = TenantValidationUtil.getRequiredTenantId();
        List<UserImportResult.RowError> errors = new ArrayList<>();
        int created = 0;

        for (int i = 0; i < dataRows.size(); i++) {
            int rowNumber = i + 2;
            List<String> row = dataRows.get(i);
            String username = value(row, header.get("username"));
            try {
                CreateUserRequest request = toRequest(row, header);
                if (dryRun) {
                    validateOnly(tenantId, request);
                } else {
                    userCommandService.addUser(request);
                }
                created++;
            } catch (BusinessException ex) {
                errors.add(UserImportResult.RowError.builder()
                        .rowNumber(rowNumber).username(username)
                        .code(ex.getCode()).message(ex.getMessage()).build());
            } catch (Exception ex) {
                log.warn("用户导入第{}行失败: {}", rowNumber, ex.getMessage());
                errors.add(UserImportResult.RowError.builder()
                        .rowNumber(rowNumber).username(username)
                        .code(ResultCode.SERVER_ERROR.getCode()).message("导入失败").build());
            }
        }

        return UserImportResult.builder()
                .total(dataRows.size())
                .created(created)
                .failed(errors.size())
                .errors(errors)
                .build();
    }

    public byte[] export(UserQueryRequest request, boolean includeSensitive) {
        boolean canViewSensitive = permissionChecker.hasPermission(
                SecurityUtils.getCurrentUserUuid().orElse(null), "system:user:export:sensitive");
        if (includeSensitive && !canViewSensitive) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(),
                    "缺少导出敏感字段权限");
        }
        boolean reveal = includeSensitive;

        StringBuilder csv = new StringBuilder("\uFEFF").append(EXPORT_HEADER);
        int collected = 0;
        int pageNum = 1;
        request.setSize(100);

        while (collected < MAX_EXPORT_ROWS) {
            request.setPage(pageNum);
            Page<UserSummaryDTO> page = userQueryService.listUsers(request);
            List<UserSummaryDTO> records = page.getRecords();
            if (records == null || records.isEmpty()) {
                break;
            }
            for (UserSummaryDTO user : records) {
                if (collected >= MAX_EXPORT_ROWS) {
                    break;
                }
                csv.append(CsvUtils.escape(user.getUsername())).append(',')
                        .append(CsvUtils.escape(reveal ? user.getRealName()
                                : SensitiveType.NAME.desensitize(user.getRealName()))).append(',')
                        .append(CsvUtils.escape(reveal ? user.getPhone()
                                : SensitiveType.MOBILE.desensitize(user.getPhone()))).append(',')
                        .append(CsvUtils.escape(reveal ? user.getEmail()
                                : SensitiveType.EMAIL.desensitize(user.getEmail()))).append(',')
                        .append(CsvUtils.escape(String.valueOf(user.getStatus()))).append(',')
                        .append(CsvUtils.escape(user.getDeptName())).append(',')
                        .append(CsvUtils.escape(user.getRoleNames() != null ? String.join(";", user.getRoleNames()) : "")).append(',')
                        .append(CsvUtils.escape(format(user.getCreateTime()))).append(',')
                        .append(CsvUtils.escape(format(user.getLastLoginTime()))).append(',')
                        .append(CsvUtils.escape(user.getRemark())).append('\n');
                collected++;
            }
            if (collected >= page.getTotal()) {
                break;
            }
            pageNum++;
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void validateOnly(UUID tenantId, CreateUserRequest request) {
        if (userMapper.existsByUsername(tenantId, request.getUsername())) {
            throw new BusinessException(ResultCode.USER_EXIST.getCode(), "用户名已存在");
        }
        if (request.getPhone() != null && userMapper.existsByPhone(tenantId, request.getPhone(), null)) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "手机号已被使用");
        }
        if (request.getEmail() != null && userMapper.existsByEmail(tenantId, request.getEmail(), null)) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "邮箱已被使用");
        }
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            passwordPolicyService.validate(request.getPassword());
        }
    }

    private CreateUserRequest toRequest(List<String> row, Map<String, Integer> header) {
        String username = value(row, header.get("username"));
        String realName = value(row, header.get("realname"));
        if (username == null || username.isBlank()) {
            throw new BusinessException(ResultCode.VALIDATION_FAILED.getCode(), "用户名不能为空");
        }
        if (realName == null || realName.isBlank()) {
            throw new BusinessException(ResultCode.VALIDATION_FAILED.getCode(), "真实姓名不能为空");
        }

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername(username);
        request.setPassword(value(row, header.get("password")));
        request.setRealName(realName);
        request.setEmail(value(row, header.get("email")));
        request.setPhone(value(row, header.get("phone")));
        request.setRemark(value(row, header.get("remark")));

        String deptId = value(row, header.get("deptid"));
        if (deptId != null) {
            try {
                request.setDeptId(UUID.fromString(deptId));
            } catch (IllegalArgumentException ex) {
                throw new BusinessException(ResultCode.VALIDATION_FAILED.getCode(), "deptId 格式不正确");
            }
        }

        Set<ConstraintViolation<CreateUserRequest>> violations = VALIDATOR.validate(request);
        if (!violations.isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATION_FAILED.getCode(),
                    violations.iterator().next().getMessage());
        }
        return request;
    }

    private Map<String, Integer> headerIndex(List<String> headerRow) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < headerRow.size(); i++) {
            String key = headerRow.get(i) == null ? "" : headerRow.get(i).trim().toLowerCase();
            index.put(key, i);
        }
        if (!index.containsKey("username") || !index.containsKey("realname")) {
            throw new BusinessException(ResultCode.VALIDATION_FAILED.getCode(),
                    "模板表头必须包含 username 与 realName");
        }
        return index;
    }

    private String value(List<String> row, Integer index) {
        if (index == null || index >= row.size()) {
            return null;
        }
        String value = row.get(index);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String format(LocalDateTime time) {
        return time == null ? "" : TIME_FORMAT.format(time);
    }
}
