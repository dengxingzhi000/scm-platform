package com.scmcloud.warehouse.engine;

import com.scmcloud.decision.engine.ClusteringEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class WavePickingEngine implements ClusteringEngine<WaveInput, WaveOutput.Wave> {

    private static final int MAX_ORDERS_PER_WAVE = 20;
    private static final Map<String, Set<String>> ADJACENT_REGIONS = Map.of(
            "华东", Set.of("华中"),
            "华南", Set.of("西南"),
            "华北", Set.of("东北"),
            "华中", Set.of("华东"),
            "西南", Set.of("华南"),
            "东北", Set.of("华北")
    );

    @Override
    public List<WaveOutput.Wave> cluster(WaveInput input) {
        List<WaveOutput.Wave> waves = new ArrayList<>();

        Map<String, List<WaveInput.PendingOrder>> byCarrier = input.getOrders().stream()
                .collect(Collectors.groupingBy(WaveInput.PendingOrder::getCarrierId));

        for (var carrierGroup : byCarrier.entrySet()) {
            Map<String, List<WaveInput.PendingOrder>> byRegion = carrierGroup.getValue().stream()
                    .collect(Collectors.groupingBy(WaveInput.PendingOrder::getRegion));

            for (var regionGroup : byRegion.entrySet()) {
                List<WaveInput.PendingOrder> sorted = regionGroup.getValue().stream()
                        .sorted(Comparator.comparingInt(WaveInput.PendingOrder::getPriority).reversed())
                        .toList();

                for (int i = 0; i < sorted.size(); i += MAX_ORDERS_PER_WAVE) {
                    List<WaveInput.PendingOrder> waveOrders = sorted.subList(
                            i, Math.min(i + MAX_ORDERS_PER_WAVE, sorted.size()));

                    WaveOutput.Wave wave = new WaveOutput.Wave();
                    wave.setWaveId("WAVE-" + UUID.randomUUID().toString().substring(0, 8));
                    wave.setOrderIds(waveOrders.stream()
                            .map(WaveInput.PendingOrder::getOrderId)
                            .toList());
                    wave.setOrderCount(waveOrders.size());
                    wave.setTotalSkuCount(waveOrders.stream()
                            .mapToInt(o -> o.getSkuIds().size())
                            .sum());
                    waves.add(wave);
                }
            }
        }

        return waves;
    }

    @Override
    public List<WaveOutput.Wave> decide(WaveInput input) {
        return cluster(input);
    }

    @Override
    public String engineType() { return "WAVE_PICKING"; }
}
