package com.pc.demo.orchestratorapi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pc.demo.orchestratorapi.service.InvokeAPI;

@RestController
@RequestMapping(path = "/test")
public class Controller {

	@Autowired
	InvokeAPI invokeAPI;

	@Autowired
	Environment env;

	@GetMapping
	public String getSampleResponse() {
		return "API invocation successful";
	}

	@PostMapping("/search")
	public String search() {

		String clientId = env.getProperty("appId");
		String clientSecret = env.getProperty("appSecret");
		String scope = env.getProperty("scope");
		String grantType = env.getProperty("grantType");
		String orchestratorBaseURL = env.getProperty("orchestratorBaseURL");
		String certFilePath = System.getenv("certFilePath");

		System.out.println("\n==== Environment Variables ====");
		if (clientId != null && clientId.length() > 5) {
			System.out.println("clientId => " + clientId.substring(0, 4));
		} else {
			System.out.println("clientId not available");
		}
		if (clientSecret != null && clientSecret.length() > 5) {
			System.out.println("clientSecret => " + clientSecret.substring(0, 4));
		} else {
			System.out.println("clientSecret not available");
		}
		System.out.println("scope => " + scope);
		System.out.println("grantType => " + grantType);
		System.out.println("orchestratorBaseURL => " + orchestratorBaseURL);
		System.out.println("certFilePath => " + certFilePath);
		
		System.setProperty("https.proxyHost", env.getProperty("https_proxyHost"));
		System.setProperty("https.proxyPort", env.getProperty("https_proxyPort"));
		System.out.println("proxyHost => " + System.getProperty("https.proxyHost"));
		System.out.println("proxyPort => " + System.getProperty("https.proxyPort"));

		return invokeAPI.callOrchestratorAPI(clientId, clientSecret, scope, grantType, orchestratorBaseURL,
				certFilePath);
	}
}
