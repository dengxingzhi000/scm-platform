package com.cognora.scm.purchase.domain.dto;

import com.cognora.scm.purchase.domain.entity.PurRequest;
import com.cognora.scm.purchase.domain.entity.PurRequestItem;

import java.util.List;

public record PurRequestUpsertDTO(PurRequest request, List<PurRequestItem> items) {
}
