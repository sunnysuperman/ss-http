package com.sunnysuperman.http;

import com.sunnysuperman.http.client.HttpClient;
import com.sunnysuperman.http.client.HttpTextResult;

import junit.framework.TestCase;

public class HttpClientTest extends TestCase {

	public void test_get() throws Exception {
		HttpClient client = new HttpClient(10, 30);
		client.setConnectTimeout(10);
		client.setReadTimeout(45);

		for (int i = 0; i < 10; i++) {
			HttpTextResult result = client.get("http://www.baidu.com/");
			assertTrue(result.ok());
			assertTrue(result.getBody().length() > 0);
			System.out.println(result.getBody());
		}

		client.destroy();
	}
}
