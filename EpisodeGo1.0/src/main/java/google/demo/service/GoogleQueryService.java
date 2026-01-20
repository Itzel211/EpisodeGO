package google.demo.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

@Service
public class GoogleQueryService {
    private String searchKeyword;
    private String url;
    private static final String TARGET_URL = "https://zh.wikipedia.org/zh-tw/%E9%8C%A2%E5%BE%B7%C2%B7%E8%B3%93";

    public GoogleQueryService() {
        // 空的構造函數
    }

    public HashMap<String, String> search(String searchKeyword) throws IOException {
        this.searchKeyword = searchKeyword;

        try {
            String encodeKeyword = URLEncoder.encode(searchKeyword, "utf-8");
            this.url = "https://www.google.com/search?q=" + encodeKeyword + "&oe=utf8&num=20";
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        // 重新獲取內容
        String content = fetchContent();

        // 用於存放結果和分數
        HashMap<String, String> resultMap = new HashMap<>();
        HashMap<String, Integer> scoreMap = new HashMap<>();

        // 使用 Jsoup 解析 HTML 字符串
        Document doc = Jsoup.parse(content);

        // 選擇特定元素
        Elements lis = doc.select("div.kCrYT");

        for (Element li : lis) {
            try {
                String citeUrl = li.select("a").attr("href").replace("/url?q=", "").split("&")[0];
                String title = li.select("a").select(".vvjwJb").text();
                String snippet = li.select(".BNeawe.s3v9rd.AP7Wnd").text(); // 獲取摘要內容

                if (title.equals("") && snippet.equals("")) {
                    continue;
                }

                System.out.println("Title: " + title + " , url: " + citeUrl);

                // 計算分數邏輯
                int baseScore = 1; // 每個結果的基礎分數
                int keywordWeight = 5; // 關鍵詞的權重
                int relatedWeight = 10; // 加權詞的權重

                // 計算關鍵詞和加權詞的出現次數
                int keywordCount = countOccurrences(title + snippet, searchKeyword); // 標題和摘要中關鍵詞的出現次數
                int relatedCount = countOccurrences(title + snippet, "中央公園"); // 加權詞的出現次數

                int totalScore = baseScore + keywordCount * keywordWeight + relatedCount * relatedWeight;

                // 特殊目標網址加分
                if (citeUrl.equals(TARGET_URL)) {
                    totalScore += 1000; // 優先顯示目標網址
                }

                // 儲存結果和分數
                resultMap.put(title, citeUrl);
                scoreMap.put(title, totalScore);

            } catch (IndexOutOfBoundsException e) {
                // 忽略例外
            }
        }

        // 根據分數排序結果
        return sortByScore(resultMap, scoreMap);
    }

    // 計算文字中詞語出現次數
    private int countOccurrences(String text, String keyword) {
        if (text == null || keyword == null || text.isEmpty() || keyword.isEmpty()) {
            return 0;
        }
        return text.split(keyword, -1).length - 1;
    }

    // 根據分數排序結果
    private HashMap<String, String> sortByScore(HashMap<String, String> results, HashMap<String, Integer> scores) {
        LinkedHashMap<String, String> sortedResults = new LinkedHashMap<>();

        results.entrySet().stream()
                .sorted((e1, e2) -> scores.get(e2.getKey()) - scores.get(e1.getKey())) // 根據分數降序排序
                .forEachOrdered(entry -> sortedResults.put(entry.getKey(), entry.getValue()));

        return sortedResults;
    }

    private String fetchContent() throws IOException {
        StringBuilder retVal = new StringBuilder();

        // 使用隨機的 User-Agent 避免被 Google 檢測為爬蟲
        String[] userAgents = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.82 Safari/537.36",
            "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:36.0) Gecko/20100101 Firefox/36.0",
            "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.0"
        };
        String randomUserAgent = userAgents[new Random().nextInt(userAgents.length)];

        URL u = new URL(url);
        URLConnection conn = u.openConnection();
        conn.setRequestProperty("User-agent", randomUserAgent);  // 使用隨機 User-Agent

        // 不使用緩存
        conn.setRequestProperty("Cache-Control", "no-cache");
        conn.setRequestProperty("Pragma", "no-cache");

        InputStream in = conn.getInputStream();
        InputStreamReader inReader = new InputStreamReader(in, "utf-8");
        BufferedReader bufReader = new BufferedReader(inReader);
        String line;

        while ((line = bufReader.readLine()) != null) {
            retVal.append(line);
        }
        bufReader.close();

        return retVal.toString();
    }
}
