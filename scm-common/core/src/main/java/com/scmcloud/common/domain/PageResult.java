package com.scmcloud.common.domain;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

/**
 * 鍒嗛〉缁撴灉锟?
 *
 * @author Deng
 * createData 2025/10/16 15:18
 * @version 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> {
    private Long total;
    private Long pageNum;
    private Long pageSize;
    private Long pages;
    private List<T> records;

    public static <T> PageResult<T> of(Page<T> page) {
        Objects.requireNonNull(page, "page");
        return PageResult.<T>builder()
                .total(page.getTotal())
                .pageNum(page.getCurrent())
                .pageSize(page.getSize())
                .pages(page.getPages())
                .records(page.getRecords())
                .build();
    }
}
