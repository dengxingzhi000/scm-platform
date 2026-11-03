package com.scmcloud.purchase.domain.dto;

import com.scmcloud.purchase.domain.entity.PurRequest;
import com.scmcloud.purchase.domain.entity.PurRequestItem;

import java.util.List;

public record PurRequestUpsertDTO(PurRequest request, List<PurRequestItem> items) {
}
