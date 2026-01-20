package model;

public class SearchResult {
    private WebPage webPage;
    private double score;

    public SearchResult(WebPage webPage, double score) {
        this.webPage = webPage;
        this.score = score;
    }

    public SearchResult() {
    }

    public WebPage getWebPage() {
        return webPage;
    }

    public void setWebPage(WebPage webPage) {
        this.webPage = webPage;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}