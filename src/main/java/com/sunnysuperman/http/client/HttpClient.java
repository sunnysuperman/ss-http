package com.sunnysuperman.http.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.Proxy;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.sunnysuperman.commons.util.FileUtil;
import com.sunnysuperman.commons.util.FormatUtil;
import com.sunnysuperman.commons.util.StringUtil;

import okhttp3.Authenticator;
import okhttp3.ConnectionPool;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 
 * Thread-safe http client
 * 
 **/
public class HttpClient {
	private static final HttpDownloadOptions DEFAULT_DOWNLOAD_OPTIONS = new HttpDownloadOptions();
	private static final byte[] EMPTY = new byte[0];
	private static final String CONTENT_TYPE_JSON = "application/json";

	private int connectTimeout = 15;
	private int readTimeout = 20;
	private ConnectionPool connectionPool;
	private Proxy proxy;
	private Authenticator proxyAuthenticator;
	private OkHttpClient client;
	private volatile boolean destroyed;

	public HttpClient(int maxIdleConnections, long keepAliveDuration) {
		super();
		this.connectionPool = new ConnectionPool(maxIdleConnections, keepAliveDuration, TimeUnit.SECONDS);
	}

	public HttpClient(int maxIdleConnections) {
		this(maxIdleConnections, 50);
	}

	@Deprecated
	public HttpClient(ConnectionPool connectionPool) {
		super();
		this.connectionPool = connectionPool;
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public HttpClient setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
		return this;
	}

	public int getReadTimeout() {
		return readTimeout;
	}

	public HttpClient setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
		return this;
	}

	public ConnectionPool getConnectionPool() {
		return connectionPool;
	}

	public HttpClient setConnectionPool(ConnectionPool connectionPool) {
		this.connectionPool = connectionPool;
		return this;
	}

	public Proxy getProxy() {
		return proxy;
	}

	public HttpClient setProxy(Proxy proxy) {
		this.proxy = proxy;
		return this;
	}

	public Authenticator getProxyAuthenticator() {
		return proxyAuthenticator;
	}

	public void setProxyAuthenticator(Authenticator proxyAuthenticator) {
		this.proxyAuthenticator = proxyAuthenticator;
	}

	public void destroy() {
		synchronized (this) {
			if (client != null) {
				client.connectionPool().evictAll();
				destroyed = true;
			}
		}
	}

	public HttpTextResult get(String url, Map<String, ?> params, Map<String, ?> headers) throws IOException {
		Request request = getRequestBuilder(url, params, headers);
		return executeAndGetTextResult(request);
	}

	public HttpTextResult get(String url) throws IOException {
		return get(url, null, null);
	}

	public HttpTextResult post(String url, Map<String, ?> params, Map<String, ?> headers) throws IOException {
		Request.Builder builder = new Request.Builder().url(url);
		appendHeaders(builder, headers);

		FormBody.Builder bodyBuilder = new FormBody.Builder();
		if (params != null) {
			for (Entry<String, ?> entry : params.entrySet()) {
				bodyBuilder.add(entry.getKey(), FormatUtil.parseString(entry.getValue(), StringUtil.EMPTY));
			}
		}

		Request request = builder.post(bodyBuilder.build()).build();
		return executeAndGetTextResult(request);
	}

	public HttpTextResult post(String url, String mediaType, String body, Map<String, ?> headers) throws IOException {
		Request.Builder builder = new Request.Builder().url(url);
		appendHeaders(builder, headers);

		RequestBody requestBody = body == null ? RequestBody.create(null, EMPTY)
				: RequestBody.create(MediaType.parse(mediaType), body);

		Request request = builder.post(requestBody).build();
		return executeAndGetTextResult(request);
	}

	public HttpTextResult postJSON(String url, String body, Map<String, ?> headers) throws IOException {
		return post(url, CONTENT_TYPE_JSON, body, headers);
	}

	public HttpTextResult postMultipart(String url, Map<String, ?> body, Map<String, ?> headers) throws IOException {
		Request.Builder builder = new Request.Builder().url(url);
		appendHeaders(builder, headers);

		MultipartBody.Builder bodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
		for (Entry<String, ?> entry : body.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (value == null) {
				value = StringUtil.EMPTY;
			}
			if (value instanceof File) {
				File file = (File) value;
				String fileName = file.getName();
				String contentType = "application/octet-stream";
				RequestBody data = RequestBody.create(MediaType.parse(contentType), file);
				bodyBuilder.addFormDataPart(key, fileName, data);
			} else if (value instanceof HttpUploadFile) {
				HttpUploadFile file = (HttpUploadFile) value;
				String fileName = file.getFileName() != null ? file.getFileName() : file.getFile().getName();
				String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
				RequestBody data = RequestBody.create(MediaType.parse(contentType), file.getFile());
				bodyBuilder.addFormDataPart(key, fileName, data);
			} else {
				bodyBuilder.addFormDataPart(key, FormatUtil.parseString(value));
			}
		}
		Request request = builder.post(bodyBuilder.build()).build();
		return executeAndGetTextResult(request);
	}

	public boolean download(String url, OutputStream out) throws IOException {
		return download(url, out, null);
	}

	public boolean download(String url, OutputStream out, HttpDownloadOptions options) throws IOException {
		if (options == null) {
			options = DEFAULT_DOWNLOAD_OPTIONS;
		}
		Request request = getRequestBuilder(url, options.getParams(), options.getHeaders());
		Response response = null;
		InputStream in = null;
		try {
			response = execute(request);
			if (options.getMaxSize() > 0) {
				long length = FormatUtil.parseLongValue(response.header("Content-Length"), -1);
				if (length < 0 || length > options.getMaxSize()) {
					return false;
				}
			}
			if (options.isRetrieveResponseHeaders()) {
				Map<String, List<String>> headers = response.headers().toMultimap();
				Map<String, String> responseHeaders = new HashMap<>();
				for (Entry<String, List<String>> entry : headers.entrySet()) {
					responseHeaders.put(entry.getKey(), entry.getValue().get(0));
				}
				options.setResponseHeaders(responseHeaders);
			}
			in = response.body().byteStream();
			FileUtil.copy(in, out);
			if (!response.isSuccessful()) {
				throw new IOException("Failed to download: " + url);
			}
			return true;
		} finally {
			FileUtil.close(in);
			FileUtil.close(out);
			FileUtil.close(response);
		}
	}

	public long getContentLength(String url, Map<String, ?> params, Map<String, ?> headers) throws IOException {
		Request.Builder reqBuilder = new Request.Builder().url(getUrl(url, params));
		if (headers != null) {
			for (Entry<String, ?> entry : headers.entrySet()) {
				reqBuilder.addHeader(entry.getKey(), entry.getValue().toString());
			}
		}
		Request request = reqBuilder.head().build();
		Response response = null;
		try {
			response = execute(request);
			return response.body().contentLength();
		} finally {
			FileUtil.close(response);
		}
	}

	public int idleConnectionCount() {
		return connectionPool.idleConnectionCount();
	}

	public int connectionCount() {
		return connectionPool.connectionCount();
	}

	private OkHttpClient createClient() {
		OkHttpClient.Builder builder = new OkHttpClient.Builder().connectTimeout(connectTimeout, TimeUnit.SECONDS)
				.readTimeout(readTimeout, TimeUnit.SECONDS);
		if (proxy != null) {
			builder.proxy(proxy);
		}
		if (proxyAuthenticator != null) {
			builder.proxyAuthenticator(proxyAuthenticator);
		}
		builder.connectionPool(Objects.requireNonNull(connectionPool));
		return builder.build();
	}

	private OkHttpClient getClient() {
		OkHttpClient localClient = client;
		if (localClient == null) {
			synchronized (this) {
				localClient = client;
				if (localClient == null) {
					localClient = client = createClient();
				}
			}
		}
		return localClient;
	}

	private void appendHeaders(Request.Builder builder, Map<String, ?> headers) {
		if (headers == null || headers.isEmpty()) {
			return;
		}
		for (Entry<String, ?> entry : headers.entrySet()) {
			builder.addHeader(entry.getKey(), FormatUtil.parseString(entry.getValue(), StringUtil.EMPTY));
		}
	}

	private String getUrl(String url, Map<String, ?> params) {
		if (params == null) {
			return url;
		}
		StringBuilder urlBuf = new StringBuilder(url);
		boolean appendQuestionMark = url.indexOf('?') < 0;
		for (Entry<String, ?> entry : params.entrySet()) {
			if (appendQuestionMark) {
				urlBuf.append('?');
				appendQuestionMark = false;
			} else {
				urlBuf.append('&');
			}
			try {
				urlBuf.append(entry.getKey()).append('=')
						.append(URLEncoder.encode(entry.getValue().toString(), StringUtil.UTF8));
			} catch (UnsupportedEncodingException e) {
				throw new UndeclaredThrowableException(e);
			}
		}
		return urlBuf.toString();
	}

	private Request getRequestBuilder(String url, Map<String, ?> params, Map<String, ?> headers) {
		Request.Builder builder = new Request.Builder().url(getUrl(url, params));
		if (headers != null) {
			for (Entry<String, ?> entry : headers.entrySet()) {
				builder.addHeader(entry.getKey(), entry.getValue().toString());
			}
		}
		return builder.build();
	}

	private Response execute(Request request) throws IOException {
		if (destroyed) {
			throw new IOException("HttpClient already destroyed");
		}
		return getClient().newCall(request).execute();
	}

	private HttpTextResult executeAndGetTextResult(Request request) throws IOException {
		Response response = null;
		try {
			response = execute(request);
			return new HttpTextResult(response.code(), response.body().string(), response.headers());
		} finally {
			FileUtil.close(response);
		}
	}
}
