package com.example.model;

public class WebPage {
    private String url;
    private String title;

    public WebPage(String url, String title) {
        this.url = url;
        this.title = title;
    }

    // Getter 方法
    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    // 若需要 Setter，可以添加
}
