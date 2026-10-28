package com.scmcloud.inventory.engine;

import org.springframework.stereotype.Component;
import com.scmcloud.decision.scoring.Scorer;
import com.scmcloud.decision.scoring.ScoringContext;

@Component
public class WarehouseScorer implements Scorer<WarehouseCandidate> {
    @Override
    public double score(WarehouseCandidate target, ScoringContext ctx) {
        int quantity = ctx.getVariable("quantity");
        if (target.getAvailableStock() >= quantity) return 100;
        if (target.getAvailableStock() == 0) return 0;
        return ((double) target.getAvailableStock() / quantity) * 100;
    }

    @Override
    public String dimension() { return "stock"; }

    @Override
    public double weight() { return 0.4; }
}
