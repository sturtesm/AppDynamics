package com.appdynamics.sample.rest.client;

import java.util.ArrayList;
import java.util.Hashtable;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.log4j.Logger;

import com.appdynamics.sample.servlet.CheckoutServlet;

public class WebClientPoolHelper {
	private static final int RETRY_FREQ_SECS = 1;

	private Hashtable<String, WebClientPool> clientPool = new Hashtable<String, WebClientPool> ();
	
    Logger logger = Logger.getLogger(WebClientPoolHelper.class);

	
    private static WebClientPoolHelper helper = null;
    
	private WebClientPoolHelper() {
		
	}
	
	public static WebClientPoolHelper getInstance() {
		if (helper == null) {
			helper = new WebClientPoolHelper();
		}
		return helper;
	}
	
	private String generateHostTargetKey(String host, String targetUrl) {
		return host + ":" + targetUrl;
	}
	
	public WebClient getWebClient(String host, String targetUrl, boolean retry, int timeoutSecs) {
		
		assert (host != null);
		assert (targetUrl != null);
		
		String hostTargetKey = generateHostTargetKey(host, targetUrl);
		
		WebClientPool pool = clientPool.get(hostTargetKey);
		
		if (pool == null) {
			logger.info("Creating a new web client pool for " + hostTargetKey);
			
			pool = new WebClientPool(host, targetUrl);
			
			clientPool.put(hostTargetKey, pool);
		}
		else {
			logger.info("Got existing pool (" + pool + ") from cache for " + hostTargetKey);
		}
		
		logger.info("WebClient Pool Cache Size: " + clientPool.size());
		
		return getWebClient(pool, retry, timeoutSecs);
	}

	private WebClient getWebClient(WebClientPool pool, boolean retry,
			int timeoutSecs) 
	{
		logger.debug("Attempting to get Web Client From Pool, timoutSecs left=" + timeoutSecs);
		
		WebClient client = pool.getWebClient();
		
		if (client == null && retry && timeoutSecs > 0) {
			
			logger.warn("Unable to get WebClient from pool (" + pool.getHost() + "/" + pool.getPath() + ") ");
			logger.warn("Trying again for up to " + timeoutSecs + " seconds");
			
			try {
				Thread.sleep(RETRY_FREQ_SECS * 1000);
			} catch (Exception e) {
				
			}
			client = getWebClient(pool, retry, timeoutSecs - RETRY_FREQ_SECS);
		}
		if (client == null && retry && timeoutSecs <= 0) {
			logger.warn("Unable to get WebClient from pool (" + pool.getHost() + "/" + pool.getPath() + ") ");
			logger.warn("Retry == true, but timeout is expired so won't retry");
		}
		else if (client == null && retry == false) {
			logger.warn("Unable to get WebClient from pool (" + pool.getHost() + "/" + pool.getPath() + ") ");
			logger.warn("Current pool size is " + pool.getUsageCount());
			logger.warn("Retry == false, so won't retry");
		}
		else {
			logger.debug("Got WebClient from pool (" + pool.getHost() + "/" + pool.getPath() + ") ");
		}
		
		logger.debug("Current pool usage is [" + pool.getUsageCount() + " / " + pool.getSize() + "] ");

		
		return client;
	}
	
	public void returnWebClient(String host, String targetUrl, WebClient client) {
		String hostTargetKey = generateHostTargetKey(host, targetUrl);

		logger.info("Attempting to return WebClient to pool: " + hostTargetKey);
		
		WebClientPool pool = clientPool.get(hostTargetKey);
		
		if (pool != null && client != null) {
			pool.returnWebCient(client);
			
			logger.info("Returnning web client for pool " + hostTargetKey + ", pool usage is now [ " + 
					pool.getUsageCount() + " / " + pool.getSize() + " ]");
		}
		else if (pool == null)
		{
			logger.error("Error returning client to pool, pool for " + hostTargetKey + " == null.");
			logger.error("Size of pool hash: " + clientPool.size());
		}
	}

	public void resetPool(String host, String path) {
		String hostTargetKey = generateHostTargetKey(host, path);

		logger.info("Attempting to return WebClient to pool: " + hostTargetKey);
		
		WebClientPool pool = clientPool.get(hostTargetKey);
		
		if (pool != null) {
			pool.seedPool();
		}
		
		logger.info("Resetting web client for pool " + hostTargetKey + ", pool usage is now [ " + 
				pool.getUsageCount() + " / " + pool.getSize() + " ]");
	}
}
