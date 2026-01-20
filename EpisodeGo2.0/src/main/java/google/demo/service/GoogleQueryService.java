package google.demo.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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

    public Map<String, String> search(String searchKeyword) throws IOException {
        // 組合搜尋關鍵字
        String combinedKeyword = "Friends " + searchKeyword;  
        // 獲取 50 筆結果
        String encoded = URLEncoder.encode(combinedKeyword, "utf-8");
        String url = "https://www.google.com/search?q=" + encoded + "&oe=utf8&num=50";

        // 抓取頁面原始 HTML
        String content = fetchContent(url);

        // 解析 HTML，提取結果
        Map<String, String> resultMap = new HashMap<>();

        Document doc = Jsoup.parse(content);
        Elements lis = doc.select("div.kCrYT");
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
                        continue;
                    }

                    String title = li.select("a").select(".vvjwJb").text();

                    if (title.isEmpty()) {
                        continue;
                    }

                    resultMap.put(title, citeUrl);
                    logger.debug("Google Search Title: {}, URL: {}", title, citeUrl);
                }
            } catch (IndexOutOfBoundsException e) {
                logger.warn("Failed to parse a search result entry: {}", e.getMessage());
            }
        }
        return resultMap;
    }

    // 抓取 Google 搜尋結果頁面的 HTML
    private String fetchContent(String urlStr) throws IOException {
        StringBuilder sb = new StringBuilder();

        String[] userAgents = {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)",
                "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:36.0) Gecko/20100101 Firefox/36.0",
                "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.0"
        };
        String randomUA = userAgents[new Random().nextInt(userAgents.length)];

        logger.debug("Fetching Google search result from URL: {}", urlStr);

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-agent", randomUA);
        conn.setRequestProperty("Cache-Control", "no-cache");
        conn.setRequestProperty("Pragma", "no-cache");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int responseCode = conn.getResponseCode();
        logger.debug("HTTP Response Code: {}", responseCode);
        if (responseCode != 200) {
            logger.warn("Non-OK HTTP response: {} for URL: {}", responseCode, urlStr);
            throw new IOException("Non-OK HTTP response: " + responseCode);
        }

        try (InputStream in = conn.getInputStream();
             BufferedReader br = new BufferedReader(new InputStreamReader(in, "utf-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            logger.debug("Successfully fetched Google search result from URL: {}", urlStr);
        } catch (IOException e) {
            logger.error("Failed to fetch Google search result from URL: {}", urlStr, e);
            throw e;
        }
        return sb.toString();
    }

    // 抓取子網頁的內容
    public String fetchPageContent(String pageUrl) throws IOException {
        StringBuilder sb = new StringBuilder();

        String[] userAgents = {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)",
                "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:36.0) Gecko/20100101 Firefox/36.0",
                "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.0"
        };
        String randomUA = userAgents[new Random().nextInt(userAgents.length)];

        logger.debug("Fetching subpage content from URL: {}", pageUrl);

        URL url = new URL(pageUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-agent", randomUA);
        conn.setRequestProperty("Cache-Control", "no-cache");
        conn.setRequestProperty("Pragma", "no-cache");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int responseCode = conn.getResponseCode();
        logger.debug("HTTP Response Code for subpage: {}, code: {}", pageUrl, responseCode);
        if (responseCode != 200) {
            logger.warn("Non-OK HTTP response: {} for subpage URL: {}", responseCode, pageUrl);
            throw new IOException("Non-OK HTTP response: " + responseCode);
        }

        try (InputStream in = conn.getInputStream();
             BufferedReader br = new BufferedReader(new InputStreamReader(in, "utf-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            logger.debug("Successfully fetched subpage content from URL: {}", pageUrl);
        } catch (IOException e) {
            logger.error("Failed to fetch subpage content from URL: {}", pageUrl, e);
            throw e;
        }
        return sb.toString();
    }
}