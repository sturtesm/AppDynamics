package com.appdynamics.sample.servlet;

import javax.servlet.http.HttpServlet;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.log4j.Logger;

import com.appdynamics.sample.rest.client.WebClientPoolHelper;

public abstract class PaypalDemoServlet extends HttpServlet {
	
	protected WebClientPoolHelper clientHelper = null;

	Logger logger = Logger.getLogger(PaypalDemoServlet.class);


	public PaypalDemoServlet() {
		clientHelper = WebClientPoolHelper.getInstance();
	}
	
	public WebClientPoolHelper getClientHelper() {
		return clientHelper;
	}

	/**
	 * Calls the PayPal auth service to get an authentication token which is used to process a payment
	 * 
	 * 
	 * @param abort
	 * @param userID TODO
	 * @return
	 * @throws Exception
	 */
	protected String callAuthService(boolean abort, String userID) throws Exception {

		Integer testInt = (abort) ? null : new Integer(10);

		String host = "http://localhost:7090";
		String service = "/service/v1/paypal/auth";

		WebClient client = 
				clientHelper.getWebClient(host, service, true, 10);

		/** 
		 * if abort == true, then testInt set to NULL and cause a NULL pointer exception which
		 * forces the the client to NOT be returned back to the pool
		 */
		logger.info("Abort == " + abort + ", testInt=" + testInt);

		//NPE possibly
		testInt.compareTo(10);

		if (client != null) {
			client.accept("text/plain", "text/html");

			String accessToken = client.get(String.class);

			clientHelper.returnWebClient(host, service, client);

			return accessToken;
		}
		else {
			throw new Exception ("Error getting Web Client to generate auth token from " + host + "/" + service);
		}
	}
}
