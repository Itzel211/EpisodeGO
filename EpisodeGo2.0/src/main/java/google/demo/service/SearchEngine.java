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
    private static final Logger logger = LoggerFactory.getLogger(SearchEngine.class);

    @Autowired
    private GoogleQueryService googleQueryService;

    // 關鍵字與權重
    private final double SIX_FRIENDS_WEIGHT = 3.0;       // 六人行
    private final double LAO_YOU_JI_WEIGHT = 3.0;        // 老友記
    private final double ACTOR_WEIGHT = 1.0;             // 演員
    private final double FICTIONAL_CHARACTER_WEIGHT = 1.5;// 虛構角色
    private final double USA_WEIGHT = 2.0;                // 美國
    private final double NEW_YORK_WEIGHT = 2.0;           // 紐約
    private final double HOLLYWOOD_WEIGHT = 1.5;          // 好萊塢
    private final double EMMY_AWARD_WEIGHT = 1.0;         // 艾美獎
    private final double APARTMENT_WEIGHT = 2.0;          // 公寓
    private final double CENTRAL_PARK_WEIGHT = 2.0;       // 中央公園
    private final double SITCOM_WEIGHT = 2.5;             // 情境喜劇
    private final double FRIEND_WEIGHT = 1.0;             // 朋友
    private final double OLD_FRIEND_WEIGHT = 1.0;         // 老友
    private final double SEASON_WEIGHT = 1.0;             // 季
    private final double MOVIE_WEIGHT = -3.0;             // 電影
    private final double MODEL_WEIGHT = -3.0;             // 模特兒
    private final double ITALY_WEIGHT = -1.0;             // 義大利
    private final double INSTAGRAM_WEIGHT = -3.0;         // instagram
    private final double FACEBOOK_WEIGHT = -3.0;          // facebook
    private final double UNIVERSITY_WEIGHT = -1.0;        // 大學
    private final double ANIMATION_WEIGHT = -2.0;         // 動畫
    private final double GAME_WEIGHT = -2.0;              // 遊戲
    private final double SPORTS_WEIGHT = -1.0;            // 運動

    // Wikipedia 額外加分
    private final double WIKIPEDIA_BONUS = 5.0;

    // 主邏輯：對搜尋結果進行分數計算並排序，確保至少有一個維基百科連結，並只顯示前 20 筆
    public List<SearchResult> searchAndSort(List<WebPage> pages, String query) {
        List<SearchResult> results = new ArrayList<>();

        for (WebPage page : pages) {
            double score = calculateScore(page, query);
            results.add(new SearchResult(page, score));
        }

        // 先排序一次
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        // 檢查是否有維基百科連結
        boolean hasWikipedia = results.stream()
                .anyMatch(result -> result.getWebPage().getUrl().toLowerCase().contains("wikipedia.org"));

        // 如果沒有維基百科連結，嘗試添加一個（若原結果中有）
        if (!hasWikipedia) {
            for (WebPage page : pages) {
                if (page.getUrl().toLowerCase().contains("wikipedia.org")) {
                    double score = calculateScore(page, query);
                    results.add(new SearchResult(page, score));
                    logger.debug("Manually added a Wikipedia link: {}", page.getUrl());
                    break;
                }
            }
            // 再次排序
            results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        }

        // 只取前 20 筆
        if (results.size() > 20) {
            return results.subList(0, 20);
        } else {
            return results;
        }
    }

    // 計算單一頁面的分數
    private double calculateScore(WebPage page, String query) {
        double baseScore = 0.0;
        String titleLower = page.getTitle().toLowerCase();

        // 關鍵字加分邏輯 (標題)
        if (titleLower.contains("six friends") || titleLower.contains("六人行") || titleLower.contains("lao you ji")) {
            baseScore += SIX_FRIENDS_WEIGHT;
        }
        if (titleLower.contains("rachel") || titleLower.contains("monica") || titleLower.contains("chandler")
                || titleLower.contains("joey") || titleLower.contains("phoebe") || titleLower.contains("ross")) {
            // 分開加分
            if (titleLower.contains("rachel")) {
                baseScore += 2.5;
            }
            if (titleLower.contains("monica")) {
                baseScore += 2.5;
            }
            if (titleLower.contains("chandler")) {
                baseScore += 2.0;
            }
            if (titleLower.contains("joey")) {
                baseScore += 2.0;
            }
            if (titleLower.contains("phoebe")) {
                baseScore += 2.0;
            }
            if (titleLower.contains("ross")) {
                baseScore += 2.0;
            }
        }
        if (titleLower.contains("central perk") || titleLower.contains("中央咖啡館")) {
            baseScore += CENTRAL_PARK_WEIGHT;
        }
        if (titleLower.contains("emmy") || titleLower.contains("艾美獎")) {
            baseScore += EMMY_AWARD_WEIGHT;
        }
        if (titleLower.contains("actor") || titleLower.contains("演員")) {
            baseScore += ACTOR_WEIGHT;
        }
        if (titleLower.contains("fictional character") || titleLower.contains("虛構角色")) {
            baseScore += FICTIONAL_CHARACTER_WEIGHT;
        }
        if (titleLower.contains("usa") || titleLower.contains("美國")) {
            baseScore += USA_WEIGHT;
        }
        if (titleLower.contains("new york") || titleLower.contains("紐約")) {
            baseScore += NEW_YORK_WEIGHT;
        }
        if (titleLower.contains("hollywood") || titleLower.contains("好萊塢")) {
            baseScore += HOLLYWOOD_WEIGHT;
        }
        if (titleLower.contains("apartment") || titleLower.contains("公寓")) {
            baseScore += APARTMENT_WEIGHT;
        }
        if (titleLower.contains("sitcom") || titleLower.contains("情境喜劇")) {
            baseScore += SITCOM_WEIGHT;
        }
        if (titleLower.contains("friend") || titleLower.contains("朋友") || titleLower.contains("老友")) {
            baseScore += FRIEND_WEIGHT;
            baseScore += OLD_FRIEND_WEIGHT;
        }
        if (titleLower.contains("season") || titleLower.contains("季")) {
            baseScore += SEASON_WEIGHT;
        }

        // 負面關鍵字
        if (titleLower.contains("movie") || titleLower.contains("電影")) {
            baseScore += MOVIE_WEIGHT;
        }
        if (titleLower.contains("model") || titleLower.contains("模特兒")) {
            baseScore += MODEL_WEIGHT;
        }
        if (titleLower.contains("italy") || titleLower.contains("義大利")) {
            baseScore += ITALY_WEIGHT;
        }
        if (titleLower.contains("instagram")) {
            baseScore += INSTAGRAM_WEIGHT;
        }
        if (titleLower.contains("facebook")) {
            baseScore += FACEBOOK_WEIGHT;
        }
        if (titleLower.contains("university") || titleLower.contains("大學")) {
            baseScore += UNIVERSITY_WEIGHT;
        }
        if (titleLower.contains("animation") || titleLower.contains("動畫")) {
            baseScore += ANIMATION_WEIGHT;
        }
        if (titleLower.contains("game") || titleLower.contains("遊戲")) {
            baseScore += GAME_WEIGHT;
        }
        if (titleLower.contains("sport") || titleLower.contains("運動")) {
            baseScore += SPORTS_WEIGHT;
        }

        // 若標題中出現用戶輸入的關鍵字，也加點分
        String[] queryKeywords = query.toLowerCase().split("\\s+");
        for (String keyword : queryKeywords) {
            if (titleLower.contains(keyword)) {
                baseScore += 1.0;
            }
        }

        // Wikipedia 額外加分
        if (page.getUrl().toLowerCase().contains("wikipedia.org")) {
            baseScore += WIKIPEDIA_BONUS;
        }

        // 子網頁內容加分
        try {
            String pageContent = googleQueryService.fetchPageContent(page.getUrl());
            double subScore = calculateSubpageScore(pageContent);
            baseScore += subScore;
            logger.debug("Page: {}, subpageScore: {}", page.getUrl(), subScore);
        } catch (IOException e) {
            logger.error("Failed to fetch subpage content for URL: {}", page.getUrl(), e);
        }

        return baseScore;
    }

    private double calculateSubpageScore(String content) {
        double subpageScore = 0.0;
        String contentLower = content.toLowerCase();

        // 依照關鍵字出現情況給分 (示範)
        if (contentLower.contains("六人行")) {
            subpageScore += 3.0;
        }
        if (contentLower.contains("老友記")) {
            subpageScore += 3.0;
        }
        if (contentLower.contains("演員")) {
            subpageScore += 1.0;
        }
        if (contentLower.contains("虛構角色")) {
            subpageScore += 1.5;
        }
        if (contentLower.contains("美國")) {
            subpageScore += 2.0;
        }
        if (contentLower.contains("紐約")) {
            subpageScore += 2.0;
        }
        if (contentLower.contains("好萊塢")) {
            subpageScore += 1.5;
        }
        if (contentLower.contains("艾美獎")) {
            subpageScore += 1.0;
        }
        if (contentLower.contains("公寓")) {
            subpageScore += 2.0;
        }
        if (contentLower.contains("中央公園")) {
            subpageScore += 2.0;
        }
        if (contentLower.contains("情境喜劇")) {
            subpageScore += 2.5;
        }
        if (contentLower.contains("朋友")) {
            subpageScore += 1.0;
        }
        if (contentLower.contains("老友")) {
            subpageScore += 1.0;
        }
        if (contentLower.contains("季")) {
            subpageScore += 1.0;
        }

        // 負面
        if (contentLower.contains("電影")) {
            subpageScore += -3.0;
        }
        if (contentLower.contains("模特兒")) {
            subpageScore += -3.0;
        }
        if (contentLower.contains("義大利")) {
            subpageScore += -1.0;
        }
        if (contentLower.contains("instagram")) {
            subpageScore += -3.0;
        }
        if (contentLower.contains("facebook")) {
            subpageScore += -3.0;
        }
        if (contentLower.contains("大學")) {
            subpageScore += -1.0;
        }
        if (contentLower.contains("動畫")) {
            subpageScore += -2.0;
        }
        if (contentLower.contains("遊戲")) {
            subpageScore += -2.0;
        }
        if (contentLower.contains("運動")) {
            subpageScore += -1.0;
        }

        logger.debug("Calculated subpage score: {}", subpageScore);
        return subpageScore;
    }
}