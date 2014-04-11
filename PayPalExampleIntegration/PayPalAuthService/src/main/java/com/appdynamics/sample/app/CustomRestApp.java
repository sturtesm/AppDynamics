package com.appdynamics.sample.app;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("/v1")
public class CustomRestApp extends Application {

	public CustomRestApp() {
		
	}
}
