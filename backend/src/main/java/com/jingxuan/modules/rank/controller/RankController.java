package com.jingxuan.modules.rank.controller;

import com.jingxuan.common.Result;
import com.jingxuan.modules.rank.dto.RankQueryRequest;
import com.jingxuan.modules.rank.service.RankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 排行榜接口
 */
@Tag(name = "排行榜", description = "查询排行榜、分类排名与手动刷新")
@RestController
@RequestMapping("/api/rank")
@RequiredArgsConstructor
public class RankController {

    private final RankService rankService;

    @Operation(summary = "获取排行榜")
    @PostMapping("/list")
    public Result<Object> getRankList(@Valid @RequestBody RankQueryRequest request) {
        Object list = rankService.getRankList(request);
        return Result.ok(list);
    }

    @Operation(summary = "获取分类排名")
    @PostMapping("/category")
    public Result<Object> getCategoryRank(@RequestParam String techStack,
                                          @RequestParam(required = false) Long batchId,
                                          @RequestParam(defaultValue = "10") int topN) {
        Object list = rankService.getCategoryRank(techStack, batchId, topN);
        return Result.ok(list);
    }

    @Operation(summary = "手动刷新排行榜")
    @PostMapping("/refresh")
    public Result<Void> refresh() {
        rankService.refreshRankCache();
        return Result.ok();
    }
}
