package com.example.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GoogleQueryService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleQueryService.class);

    /**
     * 搜尋指定關鍵字，返回標題與 URL 的映射 (Map<標題, URL>)。
     */
    public Map<String, String> search(String searchKeyword) throws IOException {
        String combinedKeyword = "Friends " + searchKeyword;
        String encoded = URLEncoder.encode(combinedKeyword, "utf-8");

        // 抓取兩頁，每頁 50 筆，共 100 筆
        Map<String, String> resultMap = new HashMap<>();
        for (int start = 0; start < 100; start += 50) {
            String url = "https://www.google.com/search?q=" + encoded + "&oe=utf8&num=50&start=" + start;
            String content = fetchContent(url);
            parseAndAddResults(content, resultMap);
        }
        return resultMap;
    }

    /**
     * 解析 HTML，將結果放進 resultMap
     */
    private void parseAndAddResults(String content, Map<String, String> resultMap) {
        Document doc = Jsoup.parse(content);
        Elements lis = doc.select("div.kCrYT");
        for (Element li : lis) {
            try {
                String href = li.select("a").attr("href");
                if (href.startsWith("/url?q=")) {
                    String citeUrl = href.replace("/url?q=", "").split("&")[0];
                    // 確保連結以 http/https 開頭
                    if (!citeUrl.startsWith("http")) {
                        citeUrl = "https://" + citeUrl;
                    }

                    // 過濾 Youtube
                    if (citeUrl.contains("youtube.com") || citeUrl.contains("youtu.be")) {
                        continue;
                    }

                    String title = li.select("a").select(".vvjwJb").text();
                    if (title.isEmpty()) {
                        continue;
                    }
                    resultMap.put(title, citeUrl);
                    logger.info("Title: {}, URL: {}", title, citeUrl);
                }
            } catch (IndexOutOfBoundsException e) {
                logger.warn("Failed to parse search result entry: {}", e.getMessage());
            }
        }
    }

    /**
     * 從 Google 搜尋頁抓取 HTML
     */
    private String fetchContent(String urlStr) throws IOException {
        StringBuilder sb = new StringBuilder();

        String[] userAgents = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)",
            "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:36.0) Gecko/20100101 Firefox/36.0",
            "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.0"
        };
        String randomUA = userAgents[new Random().nextInt(userAgents.length)];

        URL url = new URL(urlStr);
        URLConnection conn = url.openConnection();
        conn.setRequestProperty("User-agent", randomUA);
        conn.setRequestProperty("Cache-Control", "no-cache");
        conn.setRequestProperty("Pragma", "no-cache");

        try (InputStream in = conn.getInputStream();
             BufferedReader br = new BufferedReader(new InputStreamReader(in, "utf-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            logger.error("Failed to fetch content from URL: {}", urlStr, e);
            throw e;
        }
        return sb.toString();
    }
}
