package google.demo.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
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
     * 搜尋指定關鍵字，返回標題與 URL 的映射。
     *
     * @param searchKeyword 用戶輸入的搜尋關鍵字
     * @return 標題與 URL 的映射
     * @throws IOException 異常時拋出
     */
    public Map<String, String> search(String searchKeyword) throws IOException {
        // 將 "Friends" 與用戶輸入的關鍵字結合，移除 site:wikipedia.org
        String combinedKeyword = "Friends " + searchKeyword;
        String encoded = URLEncoder.encode(combinedKeyword, "utf-8");
        
        // 抓取兩頁，每頁 50 個結果，共 100 個結果
        Map<String, String> resultMap = new HashMap<>();
        for (int start = 0; start < 100; start += 50) {
            String url = "https://www.google.com/search?q=" + encoded + "&oe=utf8&num=50&start=" + start;
            String content = fetchContent(url);
            parseAndAddResults(content, resultMap);
        }
        return resultMap;
    }

    /**
     * 解析 HTML 並將結果加入到結果映射中。
     *
     * @param content    從 Google 搜尋結果頁面抓取的 HTML 內容
     * @param resultMap  用於存儲結果的映射
     */
    private void parseAndAddResults(String content, Map<String, String> resultMap) {
        Document doc = Jsoup.parse(content);
        Elements lis = doc.select("div.kCrYT"); // 根據 Google 搜尋結果的 HTML 結構選擇器，可能需要根據實際情況調整
        for (Element li : lis) {
            try {
                String href = li.select("a").attr("href");
                if (href.startsWith("/url?q=")) {
                    String citeUrl = href.replace("/url?q=", "").split("&")[0];
                    // 確保 URL 以 http 或 https 開頭
                    if (!citeUrl.startsWith("http")) {
                        citeUrl = "https://" + citeUrl;
                    }

                    // 過濾 YouTube 連結
                    if (citeUrl.contains("youtube.com") || citeUrl.contains("youtu.be")) {
                        continue; // 跳過 YouTube 連結
                    }

                    String title = li.select("a").select(".vvjwJb").text();

                    if (title.isEmpty()) {
                        continue;
                    }

                    // 加入結果
                    resultMap.put(title, citeUrl);

                    // 日誌輸出（使用 SLF4J）
                    logger.info("Title: {}, URL: {}", title, citeUrl);
                }
            } catch (IndexOutOfBoundsException e) {
                // 忽略解析失敗，並記錄警告
                logger.warn("Failed to parse a search result entry: {}", e.getMessage());
            }
        }
    }

    /**
     * 抓取指定 URL 的內容。
     *
     * @param urlStr 要抓取的 URL
     * @return 抓取到的內容
     * @throws IOException 異常時拋出
     */
    private String fetchContent(String urlStr) throws IOException {
        StringBuilder sb = new StringBuilder();

        // User-Agent 亂數，避免被 Google 辨識為爬蟲
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