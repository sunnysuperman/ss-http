package com.sunnysuperman.http.client.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.sunnysuperman.commons.exception.UnexpectedException;
import com.sunnysuperman.commons.util.FileUtil;
import com.sunnysuperman.http.client.HttpClient;
import com.sunnysuperman.http.client.HttpTextResult;
import com.sunnysuperman.http.client.RequestOptions;

class HttpClientTest {
	int port = 6666;

	@Test
	void testGet() throws Exception {
		SimpleHttpServer server = startServer();
		HttpClient client = getClient(3, 50);
		try {
			assertEquals("Method:GET,Authorization:null,Parameters:",
					client.get("http://localhost:" + port + "/testGet").getBody());
			assertEquals("Method:GET,Authorization:null,Parameters:",
					client.get("http://localhost:" + port + "/testGet", null).getBody());
			assertEquals("Method:GET,Authorization:null,Parameters:",
					client.get("http://localhost:" + port + "/testGet", (Map<String, ?>) null).getBody());
			assertEquals("Method:GET,Authorization:null,Parameters:",
					client.get("http://localhost:" + port + "/testGet", null, (RequestOptions) null).getBody());
			assertEquals("Method:GET,Authorization:null,Parameters:",
					client.get("http://localhost:" + port + "/testGet", null, mapOf("Authorization", "")).getBody());

			assertEquals("Method:GET,Authorization:null,Parameters:gender=id=123name=张三",
					client.get("http://localhost:" + port + "/testGet", mapOf("name", "张三", "id", 123, "gender", null))
							.getBody());

			assertEquals("Method:GET,Authorization:abcdef,Parameters:", client
					.get("http://localhost:" + port + "/testGet", null, mapOf("Authorization", "abcdef")).getBody());
			assertEquals("Method:GET,Authorization:abcdef,Parameters:",
					client.get("http://localhost:" + port + "/testGet", null,
							new RequestOptions().setHeaders(mapOf("Authorization", "abcdef"))).getBody());
		} finally {
			server.stop();
		}
	}

	@Test
	void testPostForm() throws Exception {
		SimpleHttpServer server = startServer();
		HttpClient client = getClient(3, 50);

		try {
			assertEquals("Method:POST,Authorization:null,Parameters:",
					client.postForm("http://localhost:" + port + "/testPostForm", null).getBody());
			assertEquals("Method:POST,Authorization:null,Parameters:", client
					.postForm("http://localhost:" + port + "/testPostForm", null, (Map<String, ?>) null).getBody());
			assertEquals("Method:POST,Authorization:null,Parameters:", client
					.postForm("http://localhost:" + port + "/testPostForm", null, (RequestOptions) null).getBody());
			assertEquals("Method:POST,Authorization:null,Parameters:",
					client.postForm("http://localhost:" + port + "/testPostForm", null, mapOf("Authorization", ""))
							.getBody());

			assertEquals("Method:POST,Authorization:null,Parameters:gender=id=123name=张三",
					client.postForm("http://localhost:" + port + "/testPostForm",
							mapOf("name", "张三", "id", 123, "gender", null)).getBody());

			assertEquals("Method:POST,Authorization:null,Parameters:",
					client.postForm("http://localhost:" + port + "/testPostForm", null, mapOf("Authorization", ""))
							.getBody());
			assertEquals("Method:POST,Authorization:abcdef,Parameters:", client
					.postForm("http://localhost:" + port + "/testPostForm", null, mapOf("Authorization", "abcdef"))
					.getBody());
		} finally {
			server.stop();
		}
	}

	@Test
	void testPostJSON() throws Exception {
		SimpleHttpServer server = startServer();
		HttpClient client = getClient(3, 50);
		try {
			assertEquals("Method:POST,Authorization:null,Parameters:,Body:",
					client.postJSON("http://localhost:" + port + "/testPostJSON", null).getBody());
			assertEquals("Method:POST,Authorization:null,Parameters:,Body:", client
					.postJSON("http://localhost:" + port + "/testPostJSON", null, (Map<String, ?>) null).getBody());
			assertEquals("Method:POST,Authorization:null,Parameters:,Body:", client
					.postJSON("http://localhost:" + port + "/testPostJSON", null, (RequestOptions) null).getBody());
			assertEquals("Method:POST,Authorization:null,Parameters:,Body:",
					client.postJSON("http://localhost:" + port + "/testPostJSON", null, mapOf("Authorization", ""))
							.getBody());

			String body = "{\"name\":\"张三\",\"id\":123}";
			assertEquals("Method:POST,Authorization:null,Parameters:,Body:" + body,
					client.postJSON("http://localhost:" + port + "/testPostJSON", body).getBody());
			assertEquals("Method:POST,Authorization:abc,Parameters:,Body:" + body,
					client.postJSON("http://localhost:" + port + "/testPostJSON", body, mapOf("Authorization", "abc"))
							.getBody());
		} finally {
			server.stop();
		}
	}

	@Test
	void testPostMultipart() throws Exception {
		SimpleHttpServer server = startServer();
		HttpClient client = getClient(3, 50);
		File file = new File(HttpClientTest.class.getResource("1.jpg").getFile());
		File file2 = new File(HttpClientTest.class.getResource("2.jpg").getFile());
		FileUtil.delete(FileUtil.getFile(new String[] { System.getProperty("user.dir"), "tmp" }));
		try {
			{
				assertEquals("Method:POST,Authorization:null,Parameters:",
						client.postMultipart("http://localhost:" + port + "/testPostMultipart", mapOf("filex", file))
								.getBody());
				File uploadedFile = FileUtil.getFile(new String[] { System.getProperty("user.dir"), "tmp", "filex" });
				assertTrue(uploadedFile.length() > 0);
				assertEquals(file.length(), uploadedFile.length());
			}
			{
				assertEquals("Method:POST,Authorization:abcd,Parameters:id=123",
						client.postMultipart("http://localhost:" + port + "/testPostMultipart",
								mapOf("filey", file, "zz", file2, "id", "123"), mapOf("Authorization", "abcd"))
								.getBody());
				File uploadedFile = FileUtil.getFile(new String[] { System.getProperty("user.dir"), "tmp", "filey" });
				assertTrue(uploadedFile.length() > 0);
				assertEquals(file.length(), uploadedFile.length());

				File uploadedFile2 = FileUtil.getFile(new String[] { System.getProperty("user.dir"), "tmp", "zz" });
				assertTrue(uploadedFile2.length() > 0);
				assertEquals(file2.length(), uploadedFile2.length());
			}
		} finally {
			server.stop();
		}
	}

	@Test
	void testDownload() throws Exception {
		SimpleHttpServer server = startServer();
		HttpClient client = getClient(3, 50);
		String fileName = "1.jpg";
		File file = new File(HttpClientTest.class.getResource(fileName).getFile());
		try {
			File downloadedFile = FileUtil
					.getFile(new String[] { System.getProperty("user.dir"), "tmp", "download_" + fileName });
			boolean ok = client.download("http://localhost:" + port + "/testDownload?fileName=" + fileName,
					new FileOutputStream(downloadedFile));
			assertTrue(ok);
			assertEquals(file.length(), downloadedFile.length());
		} finally {
			server.stop();
		}
	}

	@Test
	void keepAliveDuration() throws Exception {
		int keepAliveDuration = 15;
		HttpClient client = getClient(5, keepAliveDuration);
		int num = 32;
		CountDownLatch todoWorks = new CountDownLatch(num);

		ThreadPoolExecutor executor = new ThreadPoolExecutor(num, num, 10, TimeUnit.MINUTES,
				new LinkedBlockingQueue<>());
		for (int i = 0; i < num; i++) {
			executor.execute(new Runnable() {

				@Override
				public void run() {
					try {
						client.get("http://www.baidu.com/");
						todoWorks.countDown();
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.out.println("client.connectionCount(): " + client.connectionCount());
					System.out.println("client.idleConnectionCount(): " + client.idleConnectionCount());
				}

			});
		}
		todoWorks.await();
		long t1 = System.nanoTime();
		assertTrue(client.idleConnectionCount() <= 5);
		while (client.idleConnectionCount() > 0) {
			System.out.println("====client.idleConnectionCount(): " + client.idleConnectionCount());
			System.out.println("====client.connectionCount(): " + client.connectionCount());
			await(1000);
		}
		long t2 = System.nanoTime();
		long waitDurationOfIdleConnectionsClosed = TimeUnit.NANOSECONDS.toSeconds(t2 - t1);
		System.out.println("waitDurationOfIdleConnectionsClosed: " + waitDurationOfIdleConnectionsClosed);
		assertTrue(waitDurationOfIdleConnectionsClosed <= keepAliveDuration);
	}

	@Test
	void testGetPriorHeader() throws Exception {
		SimpleHttpServer server = startServer();
		HttpClient client = getClient(3, 50);
		try {
			{
				HttpTextResult result = client.get("http://localhost:" + port + "/testGet", null,
						new RequestOptions().setRetainPriorResponseHeaders(true));
				assertNull(result.getPriorHeader("location"));
			}
			{
				HttpTextResult result = client.get("http://localhost:" + port + "/testGetPriorHeader", null,
						new RequestOptions().setRetainPriorResponseHeaders(true));
				assertEquals("http://localhost:" + port + "/testGet", result.getPriorHeader("location"));
			}
		} finally {
			server.stop();
		}
	}

	@Test
	void testFollowRedirects() throws Exception {
		SimpleHttpServer server = startServer();
		HttpClient client = getClient(3, 50).setFollowRedirects(false);
		try {
			HttpTextResult result = client.get("http://localhost:" + port + "/testGetPriorHeader", null,
					new RequestOptions().setRetainPriorResponseHeaders(true));
			assertEquals(302, result.getCode());
			assertNull(result.getPriorHeader("location"));
			assertEquals("http://localhost:" + port + "/testGet", result.getHeader("location"));
		} finally {
			server.stop();
		}
	}

	@Test
	void testDestroy() throws Exception {
		HttpClient client = getClient(5, 120);
		int num = 32;
		CountDownLatch todoWorks = new CountDownLatch(num);
		ThreadPoolExecutor executor = new ThreadPoolExecutor(num, num, 10, TimeUnit.MINUTES,
				new LinkedBlockingQueue<>());
		for (int i = 0; i < num; i++) {
			executor.execute(new Runnable() {

				@Override
				public void run() {
					try {
						client.get("http://www.baidu.com/");
					} catch (IOException e) {
						e.printStackTrace();
					}
					todoWorks.countDown();
				}

			});
		}
		todoWorks.await();

		assertTrue(client.connectionCount() > 0);
		client.destroy();
		assertEquals(0, client.connectionCount());

		try {
			client.get("http://www.baidu.com/");
			assertTrue(false);
		} catch (Exception ex) {
			assertTrue(ex.getMessage().indexOf("destroyed") >= 0);
		}
	}

	private HttpClient getClient(int maxIdleConnections, long keepAliveDuration) {
		HttpClient client = new HttpClient(maxIdleConnections, keepAliveDuration);
		client.setConnectTimeout(10);
		client.setReadTimeout(45);
		return client;
	}

	private SimpleHttpServer startServer() throws Exception {
		SimpleHttpServer server = new SimpleHttpServer(port);
		server.start();
		return server;
	}

	@SuppressWarnings("squid:S2925")
	private void await(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static Map<String, Object> mapOf(Object... t) {
		if (t == null || t.length <= 0) {
			return Collections.emptyMap();
		}
		if (t.length % 2 != 0) {
			throw new UnexpectedException("illegal args count");
		}
		Map<String, Object> params = new HashMap<>(t.length);
		for (int i = 0; i < t.length; i += 2) {
			if (t[i] == null || !t[i].getClass().equals(String.class)) {
				throw new UnexpectedException("illegal arg: " + t[i] + "at " + i);
			}
			String key = t[i].toString();
			Object value = t[i + 1];
			params.put(key, value);
		}
		return params;
	}
}
