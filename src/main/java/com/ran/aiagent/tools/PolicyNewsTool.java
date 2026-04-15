package com.ran.aiagent.tools;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 时政新闻获取工具
 *
 * <p>负责从公开新闻源采集过去 24 小时内的国内外重要新闻。
 * <p>默认覆盖五大维度（当用户未指定话题时使用）：
 * <ul>
 *   <li>国内政治：高层动态、会议决议、法律法规</li>
 *   <li>宏观经济：央行/财政部政策、经济数据、产业规划（非股市导向）</li>
 *   <li>社会民生：司法、教育、医疗、环境、公共安全</li>
 *   <li>科技发展：重大技术突破、数字经济、航天、AI政策</li>
 *   <li>国际关系：外交动态、地缘冲突、国际组织声明、跨国合作</li>
 * </ul>
 * <p>当用户指定话题时（如"AI新闻"、"外交动态"），使用 AUTO 分类精确搜索该话题即可。
 */
@Slf4j
public class PolicyNewsTool {

    private static final String SEARCH_API_URL = "https://api.tavily.com/search";

    private final String apiKey;

    public PolicyNewsTool(String apiKey) {
        this.apiKey = apiKey;
    }

    @Tool(description = """
            News retrieval tool covering domestic and international events from the past 24 hours.

            ## DECISION RULE — read carefully before choosing category:

            - If the user asks about a **specific topic** (e.g. "AI news", "US-China trade", "SpaceX launch",
              "earthquake", "Fed rate decision"), set category = AUTO and put the topic keywords in specificFocus.
              Call this tool ONCE with AUTO — do NOT expand into multiple categories.

            - Only use the five structured categories below when the user explicitly asks for a
              **comprehensive news briefing / daily digest** with no particular topic focus:
                1. DOMESTIC_POLITICS: High-level meetings, laws/regulations, major policy announcements.
                2. MACRO_ECONOMY:     Central bank (PBOC) ops, fiscal policy, GDP/CPI/PMI, industrial plans.
                3. SOCIAL:            Judicial rulings, public health, education, environment, public safety.
                4. TECHNOLOGY:        AI, semiconductors, space, digital economy, cybersecurity.
                5. INTERNATIONAL:     Summits, conflicts, UN/WHO statements, trade agreements.

            ## Parameters
            - category:      AUTO | DOMESTIC_POLITICS | MACRO_ECONOMY | SOCIAL | TECHNOLOGY | INTERNATIONAL
            - specificFocus: When AUTO — write the exact topic keywords the user mentioned (in the user's
                             original language). When using a structured category — add sub-keywords to narrow
                             the search (e.g. "CPI data", "education reform"). Leave blank ("") only if truly
                             no sub-focus is needed.

            Returns up to 5 results per call, each with title, summary, publish time, and source URL.
            No financial analysis or investment advice.
            """)
    public String getMacroAndPolicyIntelligence(
            @ToolParam(description = """
                    Search category. Choose ONE of:
                    - AUTO                — user asked about a specific topic; put it in specificFocus
                    - DOMESTIC_POLITICS   — 国内政治
                    - MACRO_ECONOMY       — 宏观经济
                    - SOCIAL              — 社会民生
                    - TECHNOLOGY          — 科技发展
                    - INTERNATIONAL       — 国际关系
                    """)
            String category,

            @ToolParam(description = """
                    Topic keywords for the search.
                    - For AUTO: required — write exactly what the user asked about (e.g. "AI重磅新闻", "美联储加息", "马斯克").
                    - For structured categories: optional sub-focus (e.g. "CPI data", "semiconductor export control").
                    - Pass empty string "" only if no sub-focus is needed.
                    """)
            String specificFocus) {

        // 过去 24 小时：start_date = 昨天，end_date = 今天
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        String startDate = yesterday.toString(); // e.g. "2026-04-14"
        String endDate   = today.toString();     // e.g. "2026-04-15"

        String query;
        // AUTO：用户指定话题，直接用 specificFocus 构建 query，不套五件套模板
        switch (category.toUpperCase()) {
            case "AUTO":
                // 直接用用户话题关键词，让 Tavily 自由匹配，不叠加无关词
                query = specificFocus.isBlank()
                        ? "今日重要新闻"
                        : specificFocus;
                break;
            case "DOMESTIC_POLITICS":
                query = String.format("国内政治 时政 %s 政策 会议 决议", specificFocus);
                break;
            case "MACRO_ECONOMY":
                query = String.format("宏观经济 %s 经济数据 央行 财政政策 产业动态", specificFocus);
                break;
            case "SOCIAL":
                query = String.format("社会民生 %s 司法 医疗 教育 公共事件", specificFocus);
                break;
            case "TECHNOLOGY":
                query = String.format("科技发展 %s 技术突破 AI 航天 数字经济", specificFocus);
                break;
            case "INTERNATIONAL":
                query = String.format("国际新闻 %s 外交 地缘 国际组织 跨国合作", specificFocus);
                break;
            default:
                query = String.format("时政新闻 %s", specificFocus);
        }

        return executeSearch(query, startDate, endDate, today.toString());
    }

    /**
     * 执行搜索，使用 start_date / end_date 将结果精确限制在过去 24 小时内。
     *
     * @param query     搜索关键词
     * @param startDate Tavily start_date，格式 YYYY-MM-DD（昨天）
     * @param endDate   Tavily end_date，格式 YYYY-MM-DD（今天）
     * @param today     用于二次过滤 published_date 的今日日期字符串
     */
    private String executeSearch(String query, String startDate, String endDate, String today) {
        JSONObject requestBody = new JSONObject();
        requestBody.set("query", query);
        requestBody.set("api_key", apiKey);
        requestBody.set("search_depth", "basic");
        requestBody.set("topic", "news");          // 实时新闻模式，比 general 更及时
        requestBody.set("max_results", 10);
        requestBody.set("start_date", startDate);
        requestBody.set("end_date", endDate);

        try {
            String response = HttpRequest.post(SEARCH_API_URL)
                    .header("Content-Type", "application/json")
                    .body(requestBody.toString())
                    .execute()
                    .body();
            log.info("Tavily Raw Response [query={}]: {}", query, response);
            JSONObject json = JSONUtil.parseObj(response);

            if (!json.containsKey("results")) {
                String errorMsg = json.getStr("error", "Unknown API Error");
                return "Search API failed. Reason: " + errorMsg;
            }
            JSONArray organicResults = json.getJSONArray("results");
            if (organicResults == null || organicResults.isEmpty()) {
                return "过去 24 小时（" + startDate + " ~ " + endDate + "）暂无相关新闻。query=" + query;
            }

            // 二次过滤：published_date 必须 >= startDate，保留无日期的条目（由 API 层兜底）
            List<Object> filtered = organicResults.stream()
                    .filter(obj -> {
                        JSONObject item = (JSONObject) obj;
                        String pub = item.getStr("published_date", "");
                        // 无日期 → 保留；有日期 → 必须 >= startDate
                        return pub.isEmpty() || pub.compareTo(startDate) >= 0;
                    })
                    .limit(5)
                    .collect(Collectors.toList());

            if (filtered.isEmpty()) {
                return "过去 24 小时（" + startDate + " ~ " + endDate + "）暂无相关新闻（结果均为历史内容）。query=" + query;
            }

            return filtered.stream().map(obj -> {
                JSONObject item = (JSONObject) obj;
                String title = item.getStr("title", "（无标题）");
                // content 截取前 500 字，避免单条内容过长撑爆 token
                String rawContent = item.getStr("content", "（无正文）");
                String content = rawContent.length() > 500 ? rawContent.substring(0, 500) + "…" : rawContent;
                String url = item.getStr("url", "");
                String pub = item.getStr("published_date", "");
                return String.format("[%s] %s\n%s\n来源: %s", pub, title, content, url);
            }).collect(Collectors.joining("\n\n---\n\n"));

        } catch (Exception e) {
            log.error("Tavily search execution failed, query={}", query, e);
            return "Error searching intelligence: " + e.getMessage();
        }
    }
}
