package com.pc.demo.orchestratorapi.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import org.json.JSONObject;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class InvokeAPI {

//	@Autowired
//	private RestTemplate restTemplate;

	// https://stackoverflow.com/questions/36151421/could-not-autowire-fieldresttemplate-in-spring-boot-application
	@Bean
	private RestTemplate restTemplate() {
		return new RestTemplate();
	}

	public String callOrchestratorAPI(String clientId, String clientSecret, String scope, String grantType,
			String orchestratorBaseURL, String certFilePath) {
		System.out.println("Inside callOrchestratorAPI().....");

		if (certFilePath != null) {
			try {
				createTrustStore(certFilePath);
			} catch (Exception e) {
				return ("Exception while creating truststore: " + e);
			}
		}

		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", "application/json");
		headers.set("X-UIPATH-TenantName", "Default");
		headers.set("X-UIPATH-OrganizationUnitId", "1");
		headers.set("Content-Type", "application/x-www-form-urlencoded");

		String releaseKey = "";
		String accessToken = getAccessToken(clientId, clientSecret, scope, grantType, orchestratorBaseURL, headers);
		if (accessToken.startsWith("Exception")) {
			return accessToken;
		} else {
			releaseKey = getReleaseKey(accessToken, orchestratorBaseURL, headers);
		}

		System.out.println("Completed callOrchestratorAPI() !");
		return releaseKey;
	}

	private String getAccessToken(String clientId, String clientSecret, String scope, String grantType,
			String orchestratorBaseURL, HttpHeaders headers) {
		System.out.println("Inside getAccessToken().....");

		String apiUrl = orchestratorBaseURL + "identity/connect/token";
		String accessToken = null;
		try {
			// Create XML string/data for the API request
			String payload = "grant_type=" + grantType + "&client_id=" + clientId + "&client_secret=" + clientSecret
					+ "&scope=" + scope + "";

			URI uri = new URI(apiUrl);
			System.out.println("URI: " + uri.toString());
			HttpEntity<String> request = new HttpEntity<>(payload, headers);

			System.out.println("API Invocation started !");
			ResponseEntity<String> response = new RestTemplate().postForEntity(uri, request, String.class);
			System.out.println("API Invocation completed !");

			if (response != null) {
				JSONObject jsonObject = new JSONObject(response.getBody());
				accessToken = jsonObject.get("access_token").toString();
			}

			System.out.println("Response status getAccessToken(): " + response.getStatusCodeValue());
		} catch (URISyntaxException uRISyntaxException) {
			accessToken = "Exception occured while setting uri:" + uRISyntaxException;
			System.out.println(accessToken);
		} catch (Exception exception) {
			accessToken = "Exception occured while getting access token:" + exception;
			System.out.println(accessToken);
		}

		System.out.println("Completed getAccessToken() !");
		return accessToken;
	}

	private String getReleaseKey(String accessToken, String orchestratorBaseURL, HttpHeaders headers) {
		System.out.println("Inside getReleaseKey().....");

		String releaseKey = null;
		try {
			String processName = "FIPC_Maestro_POC";
			String apiUrl = orchestratorBaseURL + "odata/Releases?$Filter=Name";
			String param = " eq '" + processName + "'";
			apiUrl = apiUrl + URLEncoder.encode(param, "UTF-8");

			headers.add("Authorization", "Bearer " + accessToken);

			URI uri = new URI(apiUrl);
			System.out.println("URI: " + uri.toString());
			HttpEntity<Void> request = new HttpEntity<>(headers);

			System.out.println("API Invocation started !");
			ResponseEntity<String> response = new RestTemplate().exchange(uri, HttpMethod.GET, request, String.class);
			System.out.println("API Invocation completed !");

			if (response != null) {
				JSONObject jsonObject = new JSONObject(response.getBody());
				int processCount = jsonObject.getInt("@odata.count");
				if (processCount != 1) {
					throw new Exception("Process count for process: " + processName + " is " + processCount
							+ " which is unexpected");
				} else {
					JSONObject valueJsonObject = jsonObject.getJSONArray("value").getJSONObject(0);
					releaseKey = valueJsonObject.get("Key").toString();
				}
			}

			System.out.println("Response status getReleaseKey(): " + response.getStatusCodeValue());
		} catch (URISyntaxException uRISyntaxException) {
			releaseKey = "Exception occured while setting uri:" + uRISyntaxException;
			System.out.println(releaseKey);
		} catch (Exception exception) {
			releaseKey = "Exception occured while getting release key:" + exception;
			System.out.println(releaseKey);
		}

		System.out.println("Completed getReleaseKey() !");
		return releaseKey;
	}

	private void createTrustStore(String certFilePath)
			throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
		System.out.println("Inside createTrustStore().....");

		// https://stackoverflow.com/questions/58457063/how-to-call-ssl-endpoint-fon-aws-lambda-function

		// Declare path of trust store and create file
		String trustStorePath = "/tmp/trust";
		// try creating above directory and path if you get error no such file

		Path dir = Paths.get(trustStorePath);
		if (!Files.exists(dir)) {
			Files.createDirectories(dir);
			System.out.println("Created directory");
		}

		// Create Truststore using Key store api
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

		// locate the default truststore
		String filename = System.getProperty("java.home") + "/lib/security/cacerts".replace('/', File.separatorChar);
		try (FileInputStream fis = new FileInputStream(filename)) {
			keyStore.load(fis, "changeit".toCharArray());
		}

		Certificate cert = keyStore.getCertificate("OnPremRPAOrchChainAlias");
		if (cert != null) {
			System.out.println("Certificate already available.");
			return;
		}

		// Add Certificate to Key store
		CertificateFactory certF = CertificateFactory.getInstance("X.509");
		cert = certF.generateCertificate(new FileInputStream(certFilePath));
		keyStore.setCertificateEntry("OnPremRPAOrchChainAlias", cert);

		trustStorePath = trustStorePath + "/cacerts";

		// Write Key Store
		try (FileOutputStream out = new FileOutputStream(trustStorePath)) {
			keyStore.store(out, "changeit".toCharArray());
		}

		// Set Certificates to System properties
		System.setProperty("javax.net.ssl.trustStore", trustStorePath);
		System.setProperty("javax.net.ssl.trustStorePassword", "changeit");

		System.out.println("Completed createTrustStore() !");
	}
}
