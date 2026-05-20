package com.jingxuan.modules.dict.controller;

import com.jingxuan.common.Result;
import com.jingxuan.entity.SysDict;
import com.jingxuan.modules.dict.service.DictService;
import com.jingxuan.modules.log.service.LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据字典接口
 */
@Tag(name = "数据字典")
@RestController
@RequestMapping("/api/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;
    private final LogService logService;

    @Operation(summary = "根据字典类型获取数据")
    @GetMapping("/type/{dictType}")
    public Result<List<SysDict>> getByType(@PathVariable String dictType) {
        List<SysDict> list = dictService.getByType(dictType);
        return Result.ok(list);
    }

    @Operation(summary = "获取所有字典（按类型分组）")
    @GetMapping("/all")
    public Result<Map<String, List<SysDict>>> getAll() {
        Map<String, List<SysDict>> map = dictService.getAllGroupByType();
        return Result.ok(map);
    }

    @Operation(summary = "创建字典项")
    @PostMapping("/create")
    public Result<Long> create(@Valid @RequestBody SysDict dict) {
        Long id = dictService.createDict(dict);
        logService.recordAction("新增字典", "dictType:" + dict.getDictType(), id);
        return Result.ok(id);
    }

    @Operation(summary = "更新字典项")
    @PutMapping("/update")
    public Result<Void> update(@Valid @RequestBody SysDict dict) {
        dictService.updateDict(dict);
        logService.recordAction("修改字典", "dictType:" + dict.getDictType(), dict.getId());
        return Result.ok();
    }

    @Operation(summary = "删除字典项")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        SysDict dict = dictService.getById(id);
        String target = dict != null ? "dictType:" + dict.getDictType() : "id:" + id;
        dictService.removeById(id);
        logService.recordAction("删除字典", target, id);
        return Result.ok();
    }
}
