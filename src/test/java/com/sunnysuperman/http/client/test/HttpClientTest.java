package com.sunnysuperman.http.client.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.sunnysuperman.http.client.HttpClient;
import com.sunnysuperman.http.client.HttpTextResult;

class HttpClientTest {

	private HttpClient getClient(int maxIdleConnections, long keepAliveDuration) {
		HttpClient client = new HttpClient(maxIdleConnections, keepAliveDuration);
		client.setConnectTimeout(10);
		client.setReadTimeout(45);
		return client;
	}

	@Test
	void get() throws Exception {
		HttpClient client = getClient(11, 50);

		for (int i = 0; i < 10; i++) {
			HttpTextResult result = client.get("http://www.baidu.com/");
			assertTrue(result.ok());

			assertTrue(result.getBody().length() > 0);
			System.out.println(result.getBody());

			assertNotNull(result.getHeader("Content-Type"));
			assertEquals(result.getHeader("content-type"), result.getHeader("Content-Type"));
			assertTrue(result.getHeaders().size() > 0);
			System.out.println(result.getHeaders());
		}

		client.destroy();
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
	void destroy() throws Exception {
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

	@SuppressWarnings("squid:S2925")
	private void await(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
