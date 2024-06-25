package com.sunnysuperman.http.client;

import java.util.Map;

public class RequestOptions {
	// 请求头
	Map<String, ?> headers;
	// 是否记录跳转前响应的头
	boolean retainPriorResponseHeaders;

	public Map<String, ?> getHeaders() {
		return headers;
	}

	public RequestOptions setHeaders(Map<String, ?> headers) {
		this.headers = headers;
		return this;
	}

	public boolean isRetainPriorResponseHeaders() {
		return retainPriorResponseHeaders;
	}

	public RequestOptions setRetainPriorResponseHeaders(boolean retainPriorResponseHeaders) {
		this.retainPriorResponseHeaders = retainPriorResponseHeaders;
		return this;
	}

}
