package com.scmcloud.analytics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.analytics.domain.entity.LineageEdge;
import com.scmcloud.analytics.mapper.LineageEdgeMapper;
import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.common.exception.BusinessException;
import com.scmcloud.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Maintains the ODS → DWD → DWS → ADS → metric lineage graph.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LineageService {

    private static final Set<String> EDGE_TYPES = Set.of("TABLE", "COLUMN", "METRIC", "JOB");
    private static final Pattern NODE = Pattern.compile("[A-Za-z0-9_.:$#-]{1,160}");

    private final LineageEdgeMapper lineageEdgeMapper;

    @Master(reason = "Record lineage edge")
    @Transactional(rollbackFor = Exception.class)
    public LineageEdge record(LineageEdge edge) {
        validate(edge);
        String tenantId = TenantContextHolder.getRequiredTenantId().toString();
        String edgeType = edge.getEdgeType() == null ? "TABLE" : edge.getEdgeType().toUpperCase();

        LineageEdge existing = lineageEdgeMapper.selectOne(Wrappers.lambdaQuery(LineageEdge.class)
                .eq(LineageEdge::getTenantId, tenantId)
                .eq(LineageEdge::getUpstreamNode, edge.getUpstreamNode())
                .eq(LineageEdge::getDownstreamNode, edge.getDownstreamNode())
                .eq(LineageEdge::getEdgeType, edgeType));
        if (existing != null) {
            existing.setJobName(edge.getJobName());
            existing.setUpdateTime(OffsetDateTime.now(ZoneOffset.UTC));
            lineageEdgeMapper.updateById(existing);
            return existing;
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        LineageEdge entity = LineageEdge.builder()
                .tenantId(tenantId)
                .upstreamNode(edge.getUpstreamNode())
                .downstreamNode(edge.getDownstreamNode())
                .edgeType(edgeType)
                .jobName(edge.getJobName())
                .createTime(now)
                .updateTime(now)
                .deleted(false)
                .build();
        try {
            lineageEdgeMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException("Lineage edge already exists");
        }
        log.info("Recorded lineage edge: {} -> {} ({})", entity.getUpstreamNode(),
                entity.getDownstreamNode(), entity.getEdgeType());
        return entity;
    }

    @Slave("List lineage edges for current tenant")
    public List<LineageEdge> list() {
        return lineageEdgeMapper.selectByTenant(TenantContextHolder.getRequiredTenantId().toString());
    }

    @Slave("Read upstream lineage")
    public List<LineageEdge> upstream(String node) {
        requireNode(node);
        return lineageEdgeMapper.selectUpstream(
                TenantContextHolder.getRequiredTenantId().toString(), node);
    }

    @Slave("Read downstream lineage")
    public List<LineageEdge> downstream(String node) {
        requireNode(node);
        return lineageEdgeMapper.selectDownstream(
                TenantContextHolder.getRequiredTenantId().toString(), node);
    }

    @Master(reason = "Delete lineage edge")
    @Transactional(rollbackFor = Exception.class)
    public void delete(java.util.UUID id) {
        LineageEdge edge = lineageEdgeMapper.selectById(id);
        if (edge == null) {
            throw new BusinessException("Lineage edge not found: " + id);
        }
        lineageEdgeMapper.deleteById(id);
    }

    private void validate(LineageEdge edge) {
        if (edge == null) {
            throw new BusinessException("Lineage edge must not be null");
        }
        requireNode(edge.getUpstreamNode());
        requireNode(edge.getDownstreamNode());
        if (edge.getUpstreamNode().equals(edge.getDownstreamNode())) {
            throw new BusinessException("Lineage edge cannot be self-referencing");
        }
        if (edge.getEdgeType() != null
                && !EDGE_TYPES.contains(edge.getEdgeType().toUpperCase())) {
            throw new BusinessException("edgeType must be one of " + EDGE_TYPES);
        }
    }

    private void requireNode(String node) {
        if (node == null || !NODE.matcher(node).matches()) {
            throw new BusinessException("Invalid lineage node: " + node);
        }
    }
}
