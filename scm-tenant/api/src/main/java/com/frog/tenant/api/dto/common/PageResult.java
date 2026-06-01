package com.frog.tenant.api.dto.common;

import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PageResult<T> {

    private long total;
    private int pageNum;
    private int pageSize;
    private List<T> records;
}
