package com.sunnysuperman.http.client;

public class HttpTextResult {
    int code;
    String body;

    public HttpTextResult(int code, String body) {
        super();
        this.code = code;
        this.body = body;
    }

    public boolean ok() {
        return code == 200;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

}