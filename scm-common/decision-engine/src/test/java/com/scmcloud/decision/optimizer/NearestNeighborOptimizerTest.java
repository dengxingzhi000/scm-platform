package com.scmcloud.decision.optimizer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NearestNeighborOptimizerTest {

    record Point(String name, int x, int y) {}

    @Test
    void ordersByNearestNeighbor() {
        NearestNeighborOptimizer<Point> optimizer = new NearestNeighborOptimizer<>(
                (a, b) -> Math.sqrt(Math.pow(a.x() - b.x(), 2) + Math.pow(a.y() - b.y(), 2)));

        List<Point> points = List.of(
                new Point("A", 0, 0),
                new Point("B", 10, 0),
                new Point("C", 5, 0),
                new Point("D", 15, 0)
        );

        List<Point> result = optimizer.optimize(points);

        assertEquals("A", result.get(0).name());
        assertEquals("C", result.get(1).name());
        assertEquals("B", result.get(2).name());
        assertEquals("D", result.get(3).name());
        assertEquals("B", points.get(1).name());
    }

    @Test
    void acceptsEmptyInput() {
        NearestNeighborOptimizer<Point> optimizer = new NearestNeighborOptimizer<>(
                (a, b) -> Math.hypot(a.x() - b.x(), a.y() - b.y()));

        assertTrue(optimizer.optimize(List.of()).isEmpty());
    }

    @Test
    void returnsSinglePointWithoutComputingDistance() {
        NearestNeighborOptimizer<Point> optimizer = new NearestNeighborOptimizer<>((a, b) -> {
            throw new AssertionError("Unexpected distance calculation between " + a + " and " + b);
        });
        Point point = new Point("A", 0, 0);

        assertEquals(List.of(point), optimizer.optimize(List.of(point)));
    }
}
