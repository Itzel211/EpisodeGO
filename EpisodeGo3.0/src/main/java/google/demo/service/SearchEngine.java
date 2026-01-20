package google.demo.service;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import model.SearchResult;
import model.WebPage;

@Service
public class SearchEngine {

    // 關鍵字與權重設定（保持不變）
    private final double FRIENDS_WEIGHT = 3.0;
    private final double RACHEL_WEIGHT = 2.5;
    private final double MONICA_WEIGHT = 2.5;
    private final double CHANDLER_WEIGHT = 2.0;
    private final double JOEY_WEIGHT = 2.0;
    private final double PHOEBE_WEIGHT = 2.0;
    private final double ROSS_WEIGHT = 2.0;

    // 中英文關鍵字
    private final double CENTRAL_PERK_WEIGHT = 2.0; // 《六人行》的咖啡館
    private final double SIX_FRIENDS_WEIGHT = 2.0; // 英文名稱（降低權重）
    private final double LIU_REN_XING_WEIGHT = 1.5; // 中文名稱（降低權重）
    private final double ERIC_ASCELINE_WEIGHT = 2.5; // 主要製作人之一
    private final double DAVID_CRANE_WEIGHT = 2.5; // 主要製作人之一

    /**
     * 搜尋並排序結果，返回符合條件的結果列表。
     *
     * @param pages 搜尋到的所有 WebPage
     * @param query 用戶的搜尋查詢
     * @return 排序後的搜尋結果列表，最多 15 個，且最多有 3 個來自維基百科類似的網站
     */
    public List<SearchResult> searchAndSort(List<WebPage> pages, String query) {
        List<SearchResult> results = new ArrayList<>();

        for (WebPage page : pages) {
            double score = calculateScore(page, query);
            results.add(new SearchResult(page, score));
        }

        // 分數高 -> 排前面
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        List<SearchResult> finalResults = new ArrayList<>();
        int wikiCount = 0;
        int totalDesired = 15;
        int maxWiki = 3;

        // 1. 優先選取一個維基百科連結
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            String urlLower = result.getWebPage().getUrl().toLowerCase();
            if (urlLower.contains("wikipedia.org")) {
                finalResults.add(result);
                wikiCount++;
                results.remove(i);
                break; // 只選取一個維基百科連結
            }
        }

        // 2. 選取其他結果，最多再加入兩個維基百科連結
        for (SearchResult result : results) {
            if (finalResults.size() >= totalDesired) {
                break;
            }

            String urlLower = result.getWebPage().getUrl().toLowerCase();
            if (urlLower.contains("wikipedia.org")) {
                if (wikiCount < maxWiki) {
                    finalResults.add(result);
                    wikiCount++;
                }
            } else {
                finalResults.add(result);
            }
        }

        // 3. 如果總結果數未達 15 個，嘗試填充更多非維基百科結果
        if (finalResults.size() < totalDesired) {
            for (SearchResult result : results) {
                String urlLower = result.getWebPage().getUrl().toLowerCase();
                if (!urlLower.contains("wikipedia.org") && !finalResults.contains(result)) {
                    finalResults.add(result);
                    if (finalResults.size() >= totalDesired) {
                        break;
                    }
                }
            }
        }

        return finalResults;
    }

    /**
     * 計算每個頁面的分數。
     *
     * @param page  WebPage 物件
     * @param query 用戶的搜尋查詢
     * @return 計算後的分數
     */
    private double calculateScore(WebPage page, String query) {
        double baseScore = 0.0;
        String titleLower = page.getTitle().toLowerCase(); // 標題轉為小寫

        // 依照關鍵字給分（示範用）
        if (titleLower.contains("friends") || titleLower.contains("six friends") || titleLower.contains("六人行")) {
            baseScore += FRIENDS_WEIGHT;
        }
        if (titleLower.contains("rachel")) {
            baseScore += RACHEL_WEIGHT;
        }
        if (titleLower.contains("monica")) {
            baseScore += MONICA_WEIGHT;
        }
        if (titleLower.contains("chandler")) {
            baseScore += CHANDLER_WEIGHT;
        }
        if (titleLower.contains("joey")) {
            baseScore += JOEY_WEIGHT;
        }
        if (titleLower.contains("phoebe")) {
            baseScore += PHOEBE_WEIGHT;
        }
        if (titleLower.contains("ross")) {
            baseScore += ROSS_WEIGHT;
        }
        if (titleLower.contains("central perk") || titleLower.contains("中央咖啡館")) {
            baseScore += CENTRAL_PERK_WEIGHT;
        }
        if (titleLower.contains("liu renxing") || titleLower.contains("六人行")) { // 中英文名稱
            baseScore += LIU_REN_XING_WEIGHT;
        }
        if (titleLower.contains("eric ascieline") || titleLower.contains("eric ascièline")) { // 製作人名稱
            baseScore += ERIC_ASCELINE_WEIGHT;
        }
        if (titleLower.contains("david crane")) { // 製作人名稱
            baseScore += DAVID_CRANE_WEIGHT;
        }

        // 依照 query 中的關鍵字給分（不區分大小寫）
        String[] queryKeywords = query.toLowerCase().split("\\s+"); // 查詢字串轉為小寫並拆詞
        for (String keyword : queryKeywords) {
            if (titleLower.contains(keyword)) {
                baseScore += 1.0; // 每個出現的關鍵字加 1 分
            }
        }

        return baseScore;
    }
}