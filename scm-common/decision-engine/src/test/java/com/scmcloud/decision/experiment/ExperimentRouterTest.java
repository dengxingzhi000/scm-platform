package com.scmcloud.decision.experiment;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExperimentRouterTest {

    @Test
    void routesUserToVariantBasedOnHash() {
        ExperimentRepository repo = mock(ExperimentRepository.class);

        Variant v1 = new Variant();
        v1.setId("V1");
        v1.setTrafficPercent(50);

        Variant v2 = new Variant();
        v2.setId("V2");
        v2.setTrafficPercent(50);

        Experiment exp = new Experiment();
        exp.setVariants(List.of(v1, v2));

        when(repo.findRunningByEngineType("PRICE_COMPARISON")).thenReturn(List.of(exp));

        ExperimentRouter router = new ExperimentRouter(repo);
        Optional<Variant> result = router.route("PRICE_COMPARISON", "user123");

        assertTrue(result.isPresent());
    }
}
