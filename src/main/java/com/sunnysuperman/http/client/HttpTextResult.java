package com.sunnysuperman.http.client;

import java.util.List;
import java.util.Map;

import okhttp3.Headers;

public class HttpTextResult {
	private int code;
	private String body;
	private Headers headers;
	private Map<String, List<String>> headersMap;

	public HttpTextResult(int code, String body, Headers headers) {
		super();
		this.code = code;
		this.body = body;
		this.headers = headers;
	}

	public boolean ok() {
		return code == 200;
	}

	public String getHeader(String name) {
		return headers.get(name);
	}

	public Map<String, List<String>> getHeaders() {
		if (headersMap == null) {
			headersMap = headers.toMultimap();
		}
		return headersMap;
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