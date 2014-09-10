package org.janelia.it.jacs.compute.access.scality.examples;

import java.io.File;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;



public class PutByKey {
	public static void main(String[] args) throws Exception {
		AsyncHttpClientConfig.Builder config = new AsyncHttpClientConfig.Builder();
		config.setAllowPoolingConnections(true);
		config.setPooledConnectionIdleTimeout(15000);
		config.setMaxConnections(1000);
		config.setMaxConnectionsPerHost(1000);
		AsyncHttpClient client = new DefaultAsyncHttpClient(config.build());
		BoundRequestBuilder put = client.preparePut("http://localhost:81/proxy/bypath/etc/hosts");
		//put.addHeader("X-Scal-Force-Version", "128");
		put.setBody(new File("/etc/hosts")) ;
		Response response = put.execute().get() ;
		System.err.println("response status code="+response.getStatusCode());
		System.err.println("my ring key ="+response.getHeader("X-Scal-Ring-Key"));
		System.err.println("response header="+response.getHeaders());
		if(response.hasResponseBody())
			System.err.println("response body="+response.getResponseBody());
		client.close();
	}

}
