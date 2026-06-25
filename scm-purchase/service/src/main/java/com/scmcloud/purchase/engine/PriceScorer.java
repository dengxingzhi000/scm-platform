package com.scmcloud.purchase.engine;

import com.scmcloud.decision.scoring.Scorer;
import com.scmcloud.decision.scoring.ScoringContext;
import com.scmcloud.purchase.engine.PriceComparisonInput.SupplierQuote;

public class PriceScorer implements Scorer<SupplierQuote> {
    @Override
    public double score(SupplierQuote target, ScoringContext ctx) {
        Double minPrice = ctx.getVariable("minPrice");
        if (minPrice == null || minPrice == 0) return 0;
        return (minPrice / target.getUnitPrice().doubleValue()) * 100;
    }

    @Override
    public String dimension() { return "price"; }

    @Override
    public double weight() { return 0.4; }
}
