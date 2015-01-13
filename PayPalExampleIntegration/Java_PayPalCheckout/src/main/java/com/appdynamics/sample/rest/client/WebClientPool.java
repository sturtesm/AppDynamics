package com.appdynamics.sample.rest.client;

import java.util.ArrayList;

import org.apache.cxf.jaxrs.client.WebClient;

public class WebClientPool {
	private static final int CLIENT_POOL_MAX_SIZE = 10;
	private ArrayList<WebClient> clientPool = new ArrayList<WebClient> ();
	
	private String host;
	private String path;

	
	public WebClientPool(String host, String path) {
		this.host = host;
		this.path = path;
		
		seedPool();
	}

	public String getHost() {
		return host;
	}
	
	public String getPath() {
		return path;
	}
	
	public synchronized WebClient getWebClient() {
		
		if (clientPool.size() > 0) {
			return clientPool.remove(0);
		} 
		else {
			return null;
		}
	}
	
	public synchronized void returnWebCient(WebClient client) {
		clientPool.add(client);
	}
	
	public void seedPool() {
		
		if (clientPool == null) {
			clientPool = new ArrayList<WebClient> ();
		}
		clientPool.clear();
		
		for (int i = 0; i < CLIENT_POOL_MAX_SIZE; i++) {
			WebClient client = 
					WebClient.create(host).path(path);
			
			clientPool.add(client);
		}
	}
	
	public int getUsageCount() {
		return CLIENT_POOL_MAX_SIZE - clientPool.size();
	}
	
	public int getSize() {
		return CLIENT_POOL_MAX_SIZE;
	}
}
