package google.demo.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import model.SearchResult;
import model.WebPage;
import google.demo.service.GoogleQueryService;
import google.demo.service.SearchEngine;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // 或指定前端來源，例如 "http://localhost:3000"
public class GoogleSearchController {

    @Autowired
    private GoogleQueryService googleQueryService;

    @Autowired
    private SearchEngine searchEngine;

    /**
     * 搜尋 API，接收 GET 請求，返回符合前端需求的搜尋結果。
     *
     * @param query 用戶的搜尋查詢
     * @return 標題與 URL 的映射
     */
    @GetMapping("/search")
    public Map<String, String> search(@RequestParam("q") String query) {
        try {
            // 1. 使用 GoogleQueryService 抓取 100 個 (標題, URL) 結果
            Map<String, String> rawResults = googleQueryService.search(query);

            // 2. 建立 WebPage 物件清單
            List<WebPage> pages = new ArrayList<>();
            for (Map.Entry<String, String> entry : rawResults.entrySet()) {
                String title = entry.getKey();
                String url = entry.getValue();
                pages.add(new WebPage(url, title));
            }

            // 3. 使用 SearchEngine 執行加權邏輯，排序並過濾結果
            List<SearchResult> sortedResults = searchEngine.searchAndSort(pages, query);

            // 4. 將排序後的結果轉換為 Map
            Map<String, String> response = new LinkedHashMap<>(); // 使用 LinkedHashMap 保持插入順序
            for (SearchResult result : sortedResults) {
                String title = result.getWebPage().getTitle();
                String url = result.getWebPage().getUrl();
                response.put(title, url);
            }

            return response;

        } catch (IOException e) {
            e.printStackTrace();
            return new LinkedHashMap<>(); // 返回空的 Map
        }
    }
}