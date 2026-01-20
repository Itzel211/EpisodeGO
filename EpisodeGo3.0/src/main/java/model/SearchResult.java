package model;

public class SearchResult {
    private WebPage webPage;
    private double score;

    public SearchResult(WebPage webPage, double score) {
        this.webPage = webPage;
        this.score = score;
    }

    // Getter 方法
    public WebPage getWebPage() {
        return webPage;
    }

    public double getScore() {
        return score;
    }

    // 若需要 Setter，可以添加
}