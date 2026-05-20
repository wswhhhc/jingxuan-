package com.jingxuan.modules.sensitive.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jingxuan.common.Result;
import com.jingxuan.config.DeepSeekConfig;
import com.jingxuan.modules.sensitive.service.DeepSeekReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeepSeekReviewServiceImpl implements DeepSeekReviewService {

    private final DeepSeekConfig deepSeekConfig;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final String SYSTEM_PROMPT = """
            你是一个严格的内容安全审核助手。你的任务是对用户输入的文本进行审核，判断是否包含任何违规内容。

            以下内容均属于违规，必须返回 passed=false：
            1. 辱骂攻击：任何形式的脏话、粗口、侮辱性词汇（如傻逼、尼玛、草泥马、操你妈、垃圾、废物、去死等）、人身攻击、歧视性言论
            2. 色情低俗：色情描写、色情暗示、低俗内容
            3. 暴力血腥：暴力行为、血腥场景、恐怖内容
            4. 政治敏感：违反法律法规、政治敏感内容
            5. 广告引流：垃圾广告、推广信息、联系方式
            6. 个人信息泄漏：真实姓名、手机号、学号、QQ号、微信号、身份证号等

            请以 JSON 格式返回审核结果：
            {"passed": false, "category": "违规类别", "reason": "具体原因"}
            或
            {"passed": true}

            注意：只要文本包含任何粗口或脏话，passed 必须为 false。
            """;

    private static final int MAX_RETRIES = 2;

    @Override
    public ReviewResult review(String text, String scene) {
        if (text == null || text.isBlank()) {
            return ReviewResult.pass();
        }

        String apiKey = deepSeekConfig.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("DeepSeek API Key 未配置，跳过内容审核（仅开发环境生效）");
            return ReviewResult.pass();
        }

        Map<String, Object> requestBody = Map.of(
                "model", deepSeekConfig.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", text)
                ),
                "response_format", Map.of("type", "json_object")
        );

        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(requestBody);
        } catch (Exception e) {
            log.error("序列化请求体失败", e);
            return handleFallback("序列化异常");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(deepSeekConfig.getApiUrl()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofMillis(deepSeekConfig.getTimeout()))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        // 重试循环：首次调用 + MAX_RETRIES 次重试
        Exception lastException = null;
        Integer lastStatusCode = null;
        String lastBody = null;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                if (attempt > 0) {
                    log.info("DeepSeek API 重试第 {} 次", attempt);
                    Thread.sleep(500L * attempt); // 递增等待：500ms, 1000ms
                }

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    log.error("DeepSeek API 返回错误: status={}, body={}", response.statusCode(), response.body());
                    lastStatusCode = response.statusCode();
                    lastBody = response.body();
                    // HTTP 4xx 错误不重试（如 401 认证失败、400 请求格式错误）
                    if (response.statusCode() >= 400 && response.statusCode() < 500) {
                        return handleApiError(response.statusCode());
                    }
                    continue; // 5xx 服务端错误可重试
                }

                JsonNode root = objectMapper.readTree(response.body());
                String content = root.at("/choices/0/message/content").asText("");
                if (content.isBlank()) {
                    log.warn("DeepSeek API 返回内容为空，body={}", response.body());
                    continue; // 空内容可重试
                }

                JsonNode result = objectMapper.readTree(content);
                boolean passed = result.path("passed").asBoolean(true);
                if (passed) {
                    return ReviewResult.pass();
                }

                return ReviewResult.fail(
                        result.path("category").asText("unknown"),
                        result.path("reason").asText("内容违规")
                );

            } catch (java.net.http.HttpTimeoutException e) {
                lastException = e;
                log.warn("DeepSeek API 超时 (attempt {}/{})", attempt + 1, MAX_RETRIES + 1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return handleFallback("审核请求中断");
            } catch (Exception e) {
                lastException = e;
                log.warn("DeepSeek API 调用异常 (attempt {}/{})", attempt + 1, MAX_RETRIES + 1, e);
            }
        }

        // 所有重试耗尽，执行兜底策略
        if (lastStatusCode != null) {
            return handleApiError(lastStatusCode);
        }
        return handleFallback(lastException != null
                ? "API 调用失败（已重试" + MAX_RETRIES + "次）: " + lastException.getMessage()
                : "API 返回为空（已重试" + MAX_RETRIES + "次）");
    }

    @Override
    public Result<Void> testConnection() {
        String apiKey = deepSeekConfig.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            return Result.fail("DeepSeek API Key 未配置");
        }
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", deepSeekConfig.getModel(),
                    "messages", List.of(
                            Map.of("role", "user", "content", "ping")
                    ),
                    "max_tokens", 5
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(deepSeekConfig.getApiUrl()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return Result.ok();
            } else {
                return Result.fail("DeepSeek API 返回异常: HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            return Result.fail("DeepSeek API 连接失败: " + e.getMessage());
        }
    }

    private ReviewResult handleApiError(int statusCode) {
        String fallback = deepSeekConfig.getFallback();
        if ("bypass".equals(fallback)) {
            log.warn("DeepSeek API 错误，兜底策略为放过，status={}", statusCode);
            return ReviewResult.pass();
        }
        return ReviewResult.fail("api_error", "内容审核服务暂时不可用");
    }

    private ReviewResult handleFallback(String message) {
        String fallback = deepSeekConfig.getFallback();
        if ("bypass".equals(fallback)) {
            log.warn("DeepSeek 调用失败，兜底策略为放过: {}", message);
            return ReviewResult.pass();
        }
        return ReviewResult.fail("service_error", message);
    }
}
