package com.scmcloud.decision.optimizer;

import com.scmcloud.decision.constraint.Constraint;
import com.scmcloud.decision.scoring.ScoringContext;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class NearestNeighborOptimizer<T> implements Optimizer<List<T>, List<T>> {

    private final DistanceFunction<T> distanceFunction;

    @FunctionalInterface
    public interface DistanceFunction<T> {
        double distance(T a, T b);
    }

    public NearestNeighborOptimizer(DistanceFunction<T> distanceFunction) {
        this.distanceFunction = distanceFunction;
    }

    @Override
    public List<T> optimize(List<T> items, List<Constraint<List<T>>> constraints,
                            ScoringFunction<List<T>> scorer) {
        if (items.isEmpty()) return Collections.emptyList();

        List<T> remaining = new ArrayList<>(items);
        List<T> ordered = new ArrayList<>();
        T current = remaining.remove(0);
        ordered.add(current);

        while (!remaining.isEmpty()) {
            T finalCurrent = current;
            T nearest = remaining.stream()
                    .min(Comparator.comparingDouble(t -> distanceFunction.distance(finalCurrent, t)))
                    .orElseThrow();
            remaining.remove(nearest);
            ordered.add(nearest);
            current = nearest;
        }

        return ordered;
    }

    @Override
    public String strategy() {
        return "NEAREST_NEIGHBOR";
    }
}
