package org.janelia.it.jacs.compute.access.scality.examples;
import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.ListIterator;
import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseHeaders;
import org.asynchttpclient.HttpResponseStatus;
import org.asynchttpclient.ListenableFuture;


public class ManyConnections {

	public static void main(String[] args) throws Exception {
		AsyncHttpClientConfig.Builder config =new AsyncHttpClientConfig.Builder();
		config.setAllowPoolingConnections(true) ;
		AsyncHttpClient client = new DefaultAsyncHttpClient(config.build());
		BoundRequestBuilder request = client.prepareGet("http://localhost:81/proxy/.conf") ;
		LinkedList<ListenableFuture<String>> results = new LinkedList<ListenableFuture<String>> ();
		for(int i =0; i<1000; i++) {
			final int requestNumber =i ;
			ListenableFuture<String> f =request.execute(new AsyncHandler<String>() {
					private Throwable t = null ;
					private ByteArrayOutputStream bytes = new ByteArrayOutputStream();
					@Override
					public STATE onStatusReceived(HttpResponseStatus status) throws Exception {
						int statusCode = status.getStatusCode();
						if (statusCode != 200) {
							System.err.println("Got status code: "+statusCode+" aborting...") ;
							return STATE.ABORT;
						}
						return STATE.CONTINUE ;
					}

					@Override
					public STATE onHeadersReceived(HttpResponseHeaders h) throws Exception { return STATE.CONTINUE; }
					
					@Override
					public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
							bytes.write(bodyPart.getBodyPartBytes());
							return STATE.CONTINUE;
					}

					@Override
					public String onCompleted() throws Exception {
						if(this.t != null)
							throw new Exception(t);
						// NOTE: should probably use Content-Encoding from headers
						String result= bytes.toString("UTF-8");
						System.err.println("received n?? "+requestNumber+" body");
						/*System.err.println("n?? "+requestNumber+" body----------------------------------");
						System.err.println(result);
						System.err.println("---------------------------------------");*/
						return result ;
					}

					@Override
					public void onThrowable(Throwable t) {
						System.err.println("request n??" +requestNumber+" EXCEPTION occured "+t) ;

					}
				});
				results.add(f);
			}
		    //some kind of graceful shutdown
			while(!results.isEmpty()) {
				System.err.println("results size ="+results.size());
				ListIterator<ListenableFuture<String>> i =results.listIterator() ;
				while(i.hasNext()){
					ListenableFuture<String> someFuture = i.next() ;
					someFuture.touch();
					if(someFuture.isDone() || someFuture.isCancelled()) {
						if(someFuture.isDone()) {
							try {
								System.err.println(someFuture.get());
							} catch(Exception e) {
								System.err.println(e);
							}
						}	
						i.remove(); 
					}
				}
				Thread.sleep(1000);
				System.err.println("results size ="+results.size());
			}
		    System.err.println("shutting down");
			client.close();
		}
		
	}

