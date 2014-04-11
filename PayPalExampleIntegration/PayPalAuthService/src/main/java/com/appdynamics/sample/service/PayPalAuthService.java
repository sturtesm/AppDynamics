package com.appdynamics.sample.service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.log4j.Logger;


@Path("/paypal")
public class PayPalAuthService {

	Logger logger = Logger.getLogger(PayPalAuthService.class);
	
	public PayPalAuthService() {

	}

	@GET
	@Path("/auth")
	@Produces("text/plain")
	public String authenticate(){
		String accessToken = null;
		
		try {
			// Retrieve the access token from
			// OAuthTokenCredential by passing in
			accessToken = GenerateAccessToken.getAccessToken();
			
			logger.info("PayPal Access Token=" + accessToken);
			
		}catch (Exception e) {
			e.printStackTrace();
		}
		
		return "PayPal Access Token=" + accessToken + "...";
	}
}
