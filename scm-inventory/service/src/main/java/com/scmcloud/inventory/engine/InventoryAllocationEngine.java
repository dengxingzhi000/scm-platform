package com.scmcloud.inventory.engine;

import com.scmcloud.decision.constraint.Constraint;
import com.scmcloud.decision.constraint.ConstraintChain;
import com.scmcloud.decision.engine.*;
import com.scmcloud.decision.scoring.ScoringContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class InventoryAllocationEngine implements AllocationEngine<AllocationInput, AllocationOutput> {

    private final WarehouseScorer warehouseScorer;
    private final List<Constraint<WarehouseCandidate>> constraints;

    @Override
    public AllocationOutput allocate(AllocationInput input) {
        AllocationOutput output = new AllocationOutput();
        List<AllocationOutput.Allocation> allocations = new ArrayList<>();

        for (AllocationInput.OrderItem item : input.getItems()) {
            List<WarehouseCandidate> candidates = getCandidates(item, input);

            ConstraintChain<WarehouseCandidate> chain = new ConstraintChain<>(constraints);
            List<WarehouseCandidate> eligible = candidates.stream()
                    .filter(c -> chain.allHardConstraintsPassed(c))
                    .collect(Collectors.toList());

            if (eligible.isEmpty()) {
                log.warn("No eligible warehouse for SKU {}", item.getSkuId());
                continue;
            }

            ScoringContext ctx = new ScoringContext()
                    .withVariable("quantity", item.getQuantity())
                    .withVariable("destinationRegion", input.getDestinationRegion());

            WarehouseCandidate best = eligible.stream()
                    .max(Comparator.comparingDouble(c -> warehouseScorer.score(c, ctx)))
                    .orElseThrow();

            AllocationOutput.Allocation allocation = new AllocationOutput.Allocation();
            allocation.setSkuId(item.getSkuId());
            allocation.setWarehouseId(best.getWarehouseId());
            allocation.setQuantity(Math.min(item.getQuantity(), best.getAvailableStock()));
            allocation.setScore(warehouseScorer.score(best, ctx));
            allocations.add(allocation);
        }

        output.setAllocations(allocations);
        output.setSplitCount((int) allocations.stream()
                .map(AllocationOutput.Allocation::getWarehouseId)
                .distinct().count());
        return output;
    }

    private List<WarehouseCandidate> getCandidates(AllocationInput.OrderItem item, AllocationInput input) {
        return List.of();
    }

    @Override
    public AllocationOutput decide(AllocationInput input) {
        return allocate(input);
    }

    @Override
    public String engineType() { return "INVENTORY_ALLOCATION"; }
}
