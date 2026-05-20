package com.jingxuan.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页工具类 — 将 MP Page 转换为 PageResult
 */
public class PageUtil {

    private PageUtil() {}

    /**
     * 直接转换 Page 对象
     */
    public static <T> PageResult<T> toPageResult(Page<T> page) {
        return new PageResult<>(
                page.getRecords(),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }

    /**
     * 转换并映射 VO
     */
    public static <T, R> PageResult<R> toPageResult(Page<T> page, Function<T, R> mapper) {
        List<R> records = page.getRecords().stream()
                .map(mapper)
                .collect(Collectors.toList());
        return new PageResult<>(
                records,
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }
}
