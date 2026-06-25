package com.scmcloud.warehouse.engine;

import com.scmcloud.decision.optimizer.NearestNeighborOptimizer;
import lombok.Data;

import java.util.List;

public class PickPathOptimizer {

    @Data
    public static class Location {
        private String locationCode;
        private String zone;
        private String rack;
        private int level;
        private String skuId;
        private int quantity;
    }

    public List<Location> optimize(List<Location> locations) {
        NearestNeighborOptimizer<Location> optimizer = new NearestNeighborOptimizer<>(
                (a, b) -> distance(a, b)
        );
        return optimizer.optimize(locations, List.of(), (items, ctx) -> 0);
    }

    private double distance(Location a, Location b) {
        if (!a.getZone().equals(b.getZone())) return 3;
        if (!a.getRack().equals(b.getRack())) return 1;
        return Math.abs(a.getLevel() - b.getLevel());
    }
}
