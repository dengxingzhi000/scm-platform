package com.scmcloud.warehouse.engine;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WavePickingEngineTest {

    @Test
    void groupsOrdersByCarrierAndRegion() {
        WavePickingEngine engine = new WavePickingEngine();

        WaveInput.PendingOrder o1 = new WaveInput.PendingOrder();
        o1.setOrderId("O1");
        o1.setCarrierId("C1");
        o1.setRegion("华东");
        o1.setPriority(1);
        o1.setSkuIds(List.of("SKU1"));

        WaveInput.PendingOrder o2 = new WaveInput.PendingOrder();
        o2.setOrderId("O2");
        o2.setCarrierId("C1");
        o2.setRegion("华东");
        o2.setPriority(2);
        o2.setSkuIds(List.of("SKU2"));

        WaveInput.PendingOrder o3 = new WaveInput.PendingOrder();
        o3.setOrderId("O3");
        o3.setCarrierId("C2");
        o3.setRegion("华南");
        o3.setPriority(1);
        o3.setSkuIds(List.of("SKU3"));

        WaveInput input = new WaveInput();
        input.setWarehouseId("WH1");
        input.setOrders(List.of(o1, o2, o3));

        List<WaveOutput.Wave> waves = engine.cluster(input);

        assertEquals(2, waves.size());
        assertTrue(waves.stream().anyMatch(w -> w.getOrderIds().containsAll(List.of("O1", "O2"))));
        assertTrue(waves.stream().anyMatch(w -> w.getOrderIds().contains("O3")));
    }

    @Test
    void respectsMaxOrdersPerWave() {
        WavePickingEngine engine = new WavePickingEngine();

        List<WaveInput.PendingOrder> orders = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            WaveInput.PendingOrder o = new WaveInput.PendingOrder();
            o.setOrderId("O" + i);
            o.setCarrierId("C1");
            o.setRegion("华东");
            o.setPriority(1);
            o.setSkuIds(List.of("SKU" + i));
            orders.add(o);
        }

        WaveInput input = new WaveInput();
        input.setWarehouseId("WH1");
        input.setOrders(orders);

        List<WaveOutput.Wave> waves = engine.cluster(input);

        assertEquals(2, waves.size());
        assertEquals(20, waves.get(0).getOrderCount());
        assertEquals(5, waves.get(1).getOrderCount());
    }
}
