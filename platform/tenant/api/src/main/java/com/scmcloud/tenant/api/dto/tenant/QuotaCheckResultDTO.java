package com.scmcloud.tenant.api.dto.tenant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class QuotaCheckResultDTO {

    private boolean available;
    private int current;
    private int max;
    private String message;
}
