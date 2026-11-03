package com.cognora.platform.system.repository;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cognora.platform.decision.config.WeightProfile;
import com.cognora.platform.decision.entity.WeightProfileEntity;
import com.cognora.platform.system.mapper.WeightProfileMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MybatisWeightRepositoryTest {

    private final WeightProfileMapper weightMapper = mock(WeightProfileMapper.class);
    private final MybatisWeightRepository repository =
            new MybatisWeightRepository(weightMapper, new ObjectMapper());

    /** Lambda wrappers resolve column names through TableInfo, which a plain Mockito test never builds. */
    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), WeightProfileEntity.class);
    }

    @Test
    void blankSceneQueriesEverySceneOfTheEngine() {
        when(weightMapper.selectList(any())).thenReturn(List.of());

        repository.findActive("SCORING", "  ");

        String sql = captureSelectSql();
        assertTrue(sql.contains("engine_type"), "engine_type predicate missing: " + sql);
        assertTrue(sql.contains("active"), "active predicate missing: " + sql);
        assertFalse(sql.contains("scene"), "blank scene must not add a scene predicate: " + sql);
    }

    @Test
    void providedSceneAddsTheScenePredicate() {
        when(weightMapper.selectList(any())).thenReturn(List.of());

        repository.findActive("SCORING", "FLASH_SALE");

        assertTrue(captureSelectSql().contains("scene"));
    }

    @Test
    void findByIdParsesWeightsAndConditionsFromJson() {
        WeightProfileEntity entity = new WeightProfileEntity();
        entity.setId("weight-1");
        entity.setEngineType("SCORING");
        entity.setVersion(2);
        entity.setActive(true);
        entity.setWeights("{\"amount\":0.5,\"level\":0.5}");
        entity.setConditions("[{\"field\":\"channel\",\"operator\":\"==\",\"value\":\"APP\"}]");
        when(weightMapper.selectById("weight-1")).thenReturn(entity);

        WeightProfile profile = repository.findById("weight-1");

        assertEquals(0.5, profile.getWeights().get("amount"));
        assertEquals(1, profile.getConditions().size());
        assertEquals("channel", profile.getConditions().get(0).getField());
        assertTrue(profile.isActive());
    }

    @Test
    void saveAssignsNextVersionAndSerialisesWeights() {
        when(weightMapper.selectById("weight-new")).thenReturn(null);
        WeightProfileEntity latest = new WeightProfileEntity();
        latest.setVersion(3);
        when(weightMapper.selectList(any())).thenReturn(List.of(latest));

        WeightProfile profile = new WeightProfile();
        profile.setId("weight-new");
        profile.setEngineType("GREEDY");
        profile.setScene("S1");
        profile.setWeights(Map.of("fillRate", 0.7, "cost", 0.3));

        repository.save(profile);

        ArgumentCaptor<WeightProfileEntity> captor = ArgumentCaptor.forClass(WeightProfileEntity.class);
        verify(weightMapper).insert(captor.capture());
        assertEquals(4, captor.getValue().getVersion());
        assertTrue(captor.getValue().getWeights().contains("\"fillRate\""));
        assertNull(captor.getValue().getConditions());
    }

    @Test
    void saveUpdatesExistingProfileWithoutTouchingVersion() {
        when(weightMapper.selectById("weight-1")).thenReturn(new WeightProfileEntity());
        WeightProfile profile = new WeightProfile();
        profile.setId("weight-1");
        profile.setEngineType("SCORING");
        profile.setWeights(Map.of("amount", 1.0));

        repository.save(profile);

        ArgumentCaptor<WeightProfileEntity> captor = ArgumentCaptor.forClass(WeightProfileEntity.class);
        verify(weightMapper).updateById(captor.capture());
        verify(weightMapper, never()).insert(any(WeightProfileEntity.class));
        assertNull(captor.getValue().getVersion(), "an unsupplied version must not overwrite the stored one");
    }

    @Test
    @SuppressWarnings("unchecked")
    void activateClearsSiblingsOfTheSameSceneThenActivatesTheTarget() {
        WeightProfileEntity target = new WeightProfileEntity();
        target.setId("weight-2");
        target.setEngineType("SCORING");
        target.setScene(null);
        target.setActive(false);
        when(weightMapper.selectById("weight-2")).thenReturn(target);

        repository.activate("weight-2");

        ArgumentCaptor<Wrapper<WeightProfileEntity>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(weightMapper).update(any(WeightProfileEntity.class), wrapperCaptor.capture());
        String sql = wrapperCaptor.getValue().getSqlSegment().toLowerCase();
        assertTrue(sql.contains("scene is null"), "null scene must match scene-less siblings: " + sql);
        assertTrue(sql.contains("active"));

        ArgumentCaptor<WeightProfileEntity> entityCaptor = ArgumentCaptor.forClass(WeightProfileEntity.class);
        verify(weightMapper).updateById(entityCaptor.capture());
        assertEquals("weight-2", entityCaptor.getValue().getId());
        assertEquals(Boolean.TRUE, entityCaptor.getValue().getActive());
    }

    @Test
    void deleteByIdDelegatesToSoftDelete() {
        repository.deleteById("weight-1");
        verify(weightMapper).deleteById("weight-1");
    }

    @SuppressWarnings("unchecked")
    private String captureSelectSql() {
        ArgumentCaptor<Wrapper<WeightProfileEntity>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(weightMapper).selectList(captor.capture());
        return captor.getValue().getSqlSegment();
    }
}
