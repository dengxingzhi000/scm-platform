package com.scmcloud.purchase.engine;

import com.scmcloud.decision.constraint.Constraint;
import com.scmcloud.decision.constraint.ConstraintChain;
import com.scmcloud.decision.engine.*;
import com.scmcloud.decision.scoring.ScoringContext;
import com.scmcloud.decision.scoring.Scorer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class PriceComparisonEngine implements RankingEngine<PriceComparisonInput, PriceComparisonOutput.SupplierRanking> {

    private final List<Scorer<PriceComparisonInput.SupplierQuote>> scorers;
    private final List<Constraint<PriceComparisonInput.SupplierQuote>> constraints;

    @Override
    public List<PriceComparisonOutput.SupplierRanking> rank(PriceComparisonInput input) {
        double minPrice = input.getQuotes().stream()
                .mapToDouble(q -> q.getUnitPrice().doubleValue())
                .min()
                .orElse(0);

        ScoringContext ctx = new ScoringContext().withVariable("minPrice", minPrice);

        return input.getQuotes().stream()
                .map(quote -> {
                    ConstraintChain<PriceComparisonInput.SupplierQuote> chain = new ConstraintChain<>(constraints);
                    List<ConstraintResult> constraintResults = chain.validate(quote);

                    Map<String, Double> scores = new HashMap<>();
                    double totalScore = 0;
                    for (Scorer<PriceComparisonInput.SupplierQuote> scorer : scorers) {
                        double score = scorer.score(quote, ctx);
                        scores.put(scorer.dimension(), score);
                        totalScore += score * scorer.weight();
                    }

                    PriceComparisonOutput.SupplierRanking ranking = new PriceComparisonOutput.SupplierRanking();
                    ranking.setSupplierId(quote.getSupplierId());
                    ranking.setSupplierName(quote.getSupplierName());
                    ranking.setTotalScore(totalScore);
                    ranking.setScores(scores);
                    ranking.setConstraintResults(constraintResults);
                    return ranking;
                })
                .sorted((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()))
                .collect(Collectors.toList());
    }

    @Override
    public List<PriceComparisonOutput.SupplierRanking> decide(PriceComparisonInput input) {
        return rank(input);
    }

    @Override
    public String engineType() { return "PRICE_COMPARISON"; }
}
