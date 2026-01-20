package com.example.controller;

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

import com.example.model.SearchResult;
import com.example.model.WebPage;
import com.example.service.GoogleQueryService;
import com.example.service.SearchEngine;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // 允許任意前端網域
public class GoogleSearchController {

    @Autowired
    private GoogleQueryService googleQueryService;

    @Autowired
    private SearchEngine searchEngine;

    /**
     * 搜尋 API，接收 GET 請求。
     * 
     * 流程：
     * 1. 總是先做 GoogleQueryService 抓取 (不論輸入什麼關鍵字)
     * 2. 用 SearchEngine 進行加權排序 (若都無關 => 分數都=0)
     * 3. 若所有結果都 <= 0，回傳空 Map
     * 4. 否則正常回傳前 15 筆
     */
    @GetMapping("/search")
    public Map<String, String> search(@RequestParam("q") String query) {
        try {
            // 1. GoogleQueryService 抓原始(標題, URL)
            Map<String, String> rawResults = googleQueryService.search(query);

            // 2. 建立 WebPage 清單
            List<WebPage> pages = new ArrayList<>();
            for (Map.Entry<String, String> entry : rawResults.entrySet()) {
                String title = entry.getKey();
                String url = entry.getValue();
                pages.add(new WebPage(url, title));
            }

            // 3. 經過 SearchEngine 排序
            List<SearchResult> sortedList = searchEngine.searchAndSort(pages, query);

            // 4. 檢查結果是否「全部都 <= 5 分」
            boolean allZeroOrBelow = true;
            for (SearchResult sr : sortedList) {
                if (sr.getScore() > 5) {
                    allZeroOrBelow = false;
                    break;
                }
            }
            if (allZeroOrBelow) {
                // 全部皆不相關 => 回傳空集合
                return new LinkedHashMap<>();
            }

            // 5. 正常回傳 => Map<Title, URL>
            Map<String, String> response = new LinkedHashMap<>();
            for (SearchResult sr : sortedList) {
                String t = sr.getWebPage().getTitle();
                String u = sr.getWebPage().getUrl();
                response.put(t, u);
            }

            return response;

        } catch (IOException e) {
            e.printStackTrace();
            // 發生錯誤 => 回傳空
            return new LinkedHashMap<>();
        }
    }
}
