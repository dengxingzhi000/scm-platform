package com.scmcloud.system.repository;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.scmcloud.decision.entity.DecisionRuleEntity;
import com.scmcloud.decision.rule.DecisionRule;
import com.scmcloud.system.mapper.DecisionRuleMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MybatisRuleRepositoryTest {

    private final DecisionRuleMapper ruleMapper = mock(DecisionRuleMapper.class);
    private final MybatisRuleRepository repository = new MybatisRuleRepository(ruleMapper);

    /** Lambda wrappers resolve column names through TableInfo, which a plain Mockito test never builds. */
    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), DecisionRuleEntity.class);
    }

    @Test
    void fillsDomainDefaultsWhenColumnsAreNull() {
        DecisionRuleEntity entity = new DecisionRuleEntity();
        entity.setId("rule-1");
        entity.setEngineType("SCORING");
        entity.setRuleType("SCORING");
        entity.setExpression("#amount > 1");
        when(ruleMapper.selectList(any())).thenReturn(List.of(entity));

        List<DecisionRule> rules = repository.findByEngineType("SCORING");

        assertEquals(1, rules.size());
        DecisionRule rule = rules.get(0);
        assertEquals("rule-1", rule.getId());
        assertFalse(rule.isEnabled());
        assertEquals(100, rule.getPriority());
        assertEquals(1, rule.getVersion());
    }

    @Test
    void saveInsertsWhenIdIsUnknown() {
        when(ruleMapper.selectById("rule-new")).thenReturn(null);
        DecisionRule rule = new DecisionRule();
        rule.setId("rule-new");
        rule.setEngineType("GREEDY");
        rule.setRuleType("FILTER");
        rule.setExpression("#ok");
        rule.setEnabled(true);
        rule.setPriority(10);
        rule.setVersion(1);

        repository.save(rule);

        ArgumentCaptor<DecisionRuleEntity> captor = ArgumentCaptor.forClass(DecisionRuleEntity.class);
        verify(ruleMapper).insert(captor.capture());
        verify(ruleMapper, never()).updateById(any(DecisionRuleEntity.class));
        assertEquals("rule-new", captor.getValue().getId());
        assertEquals("GREEDY", captor.getValue().getEngineType());
        assertEquals(Boolean.TRUE, captor.getValue().getEnabled());
    }

    @Test
    void saveUpdatesWhenIdExists() {
        when(ruleMapper.selectById("rule-1")).thenReturn(new DecisionRuleEntity());
        DecisionRule rule = new DecisionRule();
        rule.setId("rule-1");
        rule.setEngineType("SCORING");
        rule.setRuleType("SCORING");
        rule.setExpression("#x");
        rule.setEnabled(false);
        rule.setPriority(20);
        rule.setVersion(2);

        repository.save(rule);

        verify(ruleMapper).updateById(any(DecisionRuleEntity.class));
        verify(ruleMapper, never()).insert(any(DecisionRuleEntity.class));
    }

    @Test
    void toggleSendsOnlyTheEnabledPatch() {
        repository.toggle("rule-1", false);

        ArgumentCaptor<DecisionRuleEntity> captor = ArgumentCaptor.forClass(DecisionRuleEntity.class);
        verify(ruleMapper).updateById(captor.capture());
        assertEquals("rule-1", captor.getValue().getId());
        assertEquals(Boolean.FALSE, captor.getValue().getEnabled());
        assertEquals(null, captor.getValue().getExpression());
    }

    @Test
    void deleteDelegatesToSoftDelete() {
        repository.delete("rule-1");
        verify(ruleMapper).deleteById("rule-1");
    }

    @Test
    void findByEngineTypeAndRuleTypeFiltersByRuleType() {
        when(ruleMapper.selectList(any())).thenReturn(List.of());

        repository.findByEngineTypeAndRuleType("SCORING", "FILTER");

        ArgumentCaptor<Wrapper<DecisionRuleEntity>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(ruleMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("engine_type"), "engine_type predicate missing: " + sql);
        assertTrue(sql.contains("rule_type"), "rule_type predicate missing: " + sql);
    }
}
