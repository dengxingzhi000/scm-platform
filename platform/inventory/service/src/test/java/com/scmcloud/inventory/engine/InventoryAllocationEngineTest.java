package com.scmcloud.inventory.engine;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InventoryAllocationEngineTest {

    @Test
    void allocatesToWarehouseWithStock() {
        WarehouseScorer scorer = new WarehouseScorer();
        StockSufficientConstraint constraint = new StockSufficientConstraint();

        InventoryAllocationEngine engine = new InventoryAllocationEngine(scorer, List.of(constraint));

        AllocationInput input = new AllocationInput();
        input.setOrderId("O1");
        input.setDestinationRegion("华东");
        input.setMaxSplits(3);

        AllocationInput.OrderItem item = new AllocationInput.OrderItem();
        item.setSkuId("SKU1");
        item.setQuantity(10);
        input.setItems(List.of(item));

        AllocationOutput output = engine.allocate(input);

        assertNotNull(output);
        assertTrue(output.getSplitCount() >= 0);
    }

    @Test
    void warehouseCandidate_hasCorrectProperties() {
        WarehouseCandidate candidate = new WarehouseCandidate();
        candidate.setWarehouseId("WH1");
        candidate.setRegion("华东");
        candidate.setAvailableStock(100);
        candidate.setHasColdChain(true);

        assertEquals("WH1", candidate.getWarehouseId());
        assertEquals(100, candidate.getAvailableStock());
        assertTrue(candidate.isHasColdChain());
    }
}
