package com.example.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import com.example.model.SearchResult;
import com.example.model.WebPage;

@Service
public class SearchEngine {

    // 關鍵字與權重
    private final double FRIENDS_WEIGHT = 3.0;
    private final double RACHEL_WEIGHT = 2.5;
    private final double MONICA_WEIGHT = 2.5;
    private final double CHANDLER_WEIGHT = 2.0;
    private final double JOEY_WEIGHT = 2.0;
    private final double PHOEBE_WEIGHT = 2.0;
    private final double ROSS_WEIGHT = 2.0;

    // 中英文關鍵字
    private final double CENTRAL_PERK_WEIGHT = 2.0; 
    private final double SIX_FRIENDS_WEIGHT = 2.0; 
    private final double LIU_REN_XING_WEIGHT = 1.5; 
    private final double ERIC_ASCELINE_WEIGHT = 2.5; 
    private final double DAVID_CRANE_WEIGHT = 2.5; 

    private final double SUB_PAGE_RATIO = 0.5;

    /**
     * 搜尋並排序結果
     * @param pages 待計分的網頁
     * @param query 查詢關鍵字
     * @return 排序後的搜尋結果（最多15筆，維基百科最多3筆，且第一筆是維基百科）
     */
    public List<SearchResult> searchAndSort(List<WebPage> pages, String query) {
        List<SearchResult> results = new ArrayList<>();

        // 1. 計算主頁 + 子網頁的總分
        for (WebPage page : pages) {
            double totalScore = calculateScoreWithSubPages(page, query);
            results.add(new SearchResult(page, totalScore));
        }

        // 2. 排序：分數由高到低
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        // 3. 挑選前15 並且維基百科最多3筆 + 第一筆維基百科
        return pickTop15WithWikiConstraint(results);
    }

    /**
     * 計算(主頁 + 2個子網頁)的分數
     */
    private double calculateScoreWithSubPages(WebPage page, String query) {
        double mainScore = calculateScore(page, query);

        // 抓取子網頁
        List<String> subLinks = pickTwoSubLinks(page.getUrl());
        double subScoreSum = 0.0;
        for (String subUrl : subLinks) {
            double subOneScore = calculateSubPageScore(subUrl, query);
            subScoreSum += subOneScore;
        }
        double adjustedSubScore = subScoreSum * SUB_PAGE_RATIO;
        return mainScore + adjustedSubScore;
    }

    private double calculateSubPageScore(String url, String query) {
        try {
            String text = fetchContent(url);
            return calcScoreFromText(text, query);
        } catch (IOException e) {
            return 0.0;
        }
    }

    /**
     * 從字串中加權計算與Friends相關之分數
     */
    private double calcScoreFromText(String text, String query) {
        double baseScore = 0.0;
        String lower = text.toLowerCase();

        // 與主頁相同關鍵字
        if (lower.contains("friends") || lower.contains("six friends") || lower.contains("六人行")) {
            baseScore += FRIENDS_WEIGHT;
        }
        if (lower.contains("rachel")) {
            baseScore += RACHEL_WEIGHT;
        }
        if (lower.contains("monica")) {
            baseScore += MONICA_WEIGHT;
        }
        if (lower.contains("chandler")) {
            baseScore += CHANDLER_WEIGHT;
        }
        if (lower.contains("joey")) {
            baseScore += JOEY_WEIGHT;
        }
        if (lower.contains("phoebe")) {
            baseScore += PHOEBE_WEIGHT;
        }
        if (lower.contains("ross")) {
            baseScore += ROSS_WEIGHT;
        }
        if (lower.contains("central perk") || lower.contains("中央咖啡館")) {
            baseScore += CENTRAL_PERK_WEIGHT;
        }
        if (lower.contains("liu renxing") || lower.contains("六人行")) {
            baseScore += LIU_REN_XING_WEIGHT;
        }
        if (lower.contains("eric ascieline")) {
            baseScore += ERIC_ASCELINE_WEIGHT;
        }
        if (lower.contains("david crane")) {
            baseScore += DAVID_CRANE_WEIGHT;
        }

        // 對 query 關鍵字加分
        String[] queryKeywords = query.toLowerCase().split("\\s+");
        for (String kw : queryKeywords) {
            if (lower.contains(kw)) {
                baseScore += 1.0;
            }
        }

        return baseScore;
    }

    /**
     * 原本計算主頁的分數
     */
    private double calculateScore(WebPage page, String query) {
        return calcScoreFromText(page.getTitle(), query);
    }

    /**
     * 隨機擷取2條子連結
     */
    private List<String> pickTwoSubLinks(String mainUrl) {
        List<String> subUrls = new ArrayList<>();
        try {
            String content = fetchContent(mainUrl);
            Document doc = Jsoup.parse(content);
            Elements links = doc.select("a[href]");
            int count = 0;
            for (org.jsoup.nodes.Element link : links) {
                String href = link.attr("abs:href");
                if (href.contains("youtube.com") || href.contains("youtu.be")) {
                    continue;
                }
                subUrls.add(href);
                count++;
                if (count >= 2) break;
            }
        } catch (IOException e) {
            // ignore
        }
        return subUrls;
    }

    private String fetchContent(String urlStr) throws IOException {
        StringBuilder sb = new StringBuilder();

        String[] userAgents = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)",
            "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:36.0) Gecko/20100101 Firefox/36.0",
            "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.0"
        };
        String randomUA = userAgents[new Random().nextInt(userAgents.length)];

        Document doc = Jsoup.connect(urlStr)
                .userAgent(randomUA)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .timeout(5000)
                .get();

        sb.append(doc.text());
        return sb.toString();
    }

    /**
     * 將結果中挑前15 並且維基百科不超過3筆，第一筆是維基百科
     */
    private List<SearchResult> pickTop15WithWikiConstraint(List<SearchResult> results) {
        List<SearchResult> finalList = new ArrayList<>();
        int wikiCount = 0;
        int totalDesired = 15;
        int maxWiki = 3;

        // 優先選一個維基百科
        for (int i = 0; i < results.size(); i++) {
            SearchResult r = results.get(i);
            String urlLower = r.getWebPage().getUrl().toLowerCase();
            if (urlLower.contains("wikipedia.org")) {
                finalList.add(r);
                wikiCount++;
                results.remove(i);
                break;
            }
        }

        // 填入其他結果
        for (SearchResult r : results) {
            if (finalList.size() >= totalDesired) break;
            String urlLower = r.getWebPage().getUrl().toLowerCase();
            if (urlLower.contains("wikipedia.org")) {
                if (wikiCount < maxWiki) {
                    finalList.add(r);
                    wikiCount++;
                }
            } else {
                finalList.add(r);
            }
        }

        // 如果未滿15，繼續塞非 wiki
        if (finalList.size() < totalDesired) {
            for (SearchResult r : results) {
                if (finalList.size() >= totalDesired) break;
                if (!finalList.contains(r)) {
                    String urlLower = r.getWebPage().getUrl().toLowerCase();
                    if (!urlLower.contains("wikipedia.org")) {
                        finalList.add(r);
                    }
                }
            }
        }

        return finalList;
    }
}
