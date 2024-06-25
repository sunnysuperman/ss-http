package com.sunnysuperman.http.client.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.TreeSet;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sunnysuperman.commons.util.FileUtil;

public class SimpleHttpServer {
	private static final Logger LOG = LoggerFactory.getLogger(Logger.class);

	int port;
	Server server;

	public SimpleHttpServer(int port) {
		super();
		this.port = port;
	}

	public void start() throws Exception {
		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setMinThreads(6);
		threadPool.setMaxThreads(12);
		threadPool.setIdleTimeout(180 * 1000);
		server = new Server(threadPool);

		ServerConnector connector0 = new ServerConnector(server);
		connector0.setPort(port);
		server.setConnectors(new Connector[] { connector0 });
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		server.setHandler(context);

		context.setContextPath("/");
		context.addServlet(new ServletHolder(new GetHandler()), "/testGet");
		context.addServlet(new ServletHolder(new PostFormHandler()), "/testPostForm");
		context.addServlet(new ServletHolder(new PostJSONHandler()), "/testPostJSON");

		ServletHolder uploadServletHolder = new ServletHolder(new PostMultipartHandler());
		uploadServletHolder.getRegistration().setMultipartConfig(new MultipartConfigElement("/tmp", // 临时文件存放目录
				1024 * 1024 * 10, // 最大上传大小，10MB
				1024 * 1024 * 20, // 请求最大大小，20MB
				1024 * 1024));
		context.addServlet(uploadServletHolder, "/testPostMultipart");
		context.addServlet(new ServletHolder(new DownloadHandler()), "/testDownload");

		context.addServlet(new ServletHolder(new GetPriorHeaderHandler()), "/testGetPriorHeader");

		server.start();
		LOG.warn("Server start at port: {}", port);
	}

	public void stop() throws Exception {
		server.stop();
	}

	private static class AbstractHandler extends HttpServlet {

		protected void setCharacterEncoding(HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {
			req.setCharacterEncoding("UTF-8");
			resp.setCharacterEncoding("UTF-8");
		}

		protected StringBuilder appendDefaultOutput(HttpServletRequest req) {
			StringBuilder buf = new StringBuilder("Method:").append(req.getMethod());
			buf.append(",Authorization:").append(req.getHeader("authorization"));
			buf.append(",Parameters:");

			TreeSet<String> keys = new TreeSet<>(req.getParameterMap().keySet());
			for (String key : keys) {
				buf.append(key).append("=").append(req.getParameter(key));
			}
			return buf;
		}

	}

	private static class GetHandler extends AbstractHandler {

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			setCharacterEncoding(req, resp);
			resp.getWriter().write(appendDefaultOutput(req).toString());
		}

	}

	private static class PostFormHandler extends AbstractHandler {

		@Override
		protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			setCharacterEncoding(req, resp);

			resp.getWriter().write(appendDefaultOutput(req).toString());
		}

	}

	private static class PostJSONHandler extends AbstractHandler {

		@Override
		protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			setCharacterEncoding(req, resp);

			StringBuilder buf = appendDefaultOutput(req);
			buf.append(",Body:").append(readRequestBody(req));
			resp.getWriter().write(buf.toString());
		}

	}

	@MultipartConfig
	private static class PostMultipartHandler extends AbstractHandler {

		@Override
		protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			setCharacterEncoding(req, resp);

			StringBuilder buf = appendDefaultOutput(req);

			for (Part part : req.getParts()) {
				String contentType = part.getHeader("content-type");
				// sample
				// content-length:123
				// content-disposition:form-data; name="file"; filename="1.jpg"
				// content-type:application/octet-stream
				if (contentType != null && "application/octet-stream".equals(contentType)) {
					File file = FileUtil
							.getFile(new String[] { System.getProperty("user.dir"), "tmp", part.getName() });
					FileUtil.delete(file);
					FileUtil.makeDirByChild(file);
					FileUtil.copy(part.getInputStream(), new FileOutputStream(file));
				}
			}
			resp.getWriter().write(buf.toString());
		}

	}

	private static class DownloadHandler extends AbstractHandler {

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			FileUtil.copy(getClass().getResourceAsStream(req.getParameter("fileName")), resp.getOutputStream());
		}

	}

	private static class GetPriorHeaderHandler extends AbstractHandler {

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.setHeader("Location", req.getRequestURL().toString().replaceAll("/testGetPriorHeader", "/testGet"));
			resp.setStatus(302);
		}

	}

	private static String readRequestBody(HttpServletRequest request) throws IOException {
		StringBuilder builder = new StringBuilder();
		String line;
		try (InputStream inputStream = request.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
		}
		return builder.toString();
	}

}
