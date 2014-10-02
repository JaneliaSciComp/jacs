package org.janelia.it.jacs.compute.access.scality;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.Body;
import org.asynchttpclient.BodyGenerator;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseHeaders;
import org.asynchttpclient.HttpResponseStatus;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;

public class ScalityDAO {
	
    private static final Logger log = Logger.getLogger(ScalityDAO.class);
    
    private static final String SCALITY_PATH_NAMESPACE = "JACS";//SystemConfigurationProperties.getString("Scality.Namespace");
    		
    private static final String SCALITY_BASE_URL = "http://s2-jrc:81/proxy";//SystemConfigurationProperties.getString("Scality.BaseURL");
	
	private static final String SCALITY_DRIVER = "bparc";//SystemConfigurationProperties.getString("Scality.Driver");
	
	private AsyncHttpClient client;
	
	public ScalityDAO() {
		int timeout = 1000*60*60;
		AsyncHttpClientConfig.Builder config = new AsyncHttpClientConfig.Builder();
		config.setAllowPoolingConnections(true);
		config.setPooledConnectionIdleTimeout(15000);
		config.setMaxConnections(1000);
		config.setMaxConnectionsPerHost(1000);
		config.setRequestTimeout(timeout);
		config.setConnectionTimeout(timeout);
		config.setReadTimeout(timeout);
		this.client = new DefaultAsyncHttpClient(config.build());
	}
	
	public void put(String scalityId, String filepath) throws Exception {
		final String url = getUrl(scalityId);
		log.info("Putting "+url+" from "+filepath);
		BoundRequestBuilder put = client.preparePut(url);
		put.setBody(new File(filepath));
		Response response = put.execute().get();
		
		log.info("PUT status code: "+response.getStatusCode());
		log.info("PUT response body: "+response.getResponseBody());
		for (String key : response.getHeaders().keySet()) {
			log.info(" "+key+": "+response.getHeaders().get(key));	
		}
		
		if (response.getStatusCode()!=200) {
			log.error("Response from Scality: "+response.getResponseBody());
			throw new Exception("Put failed for Scality#"+scalityId+" with status code "+response.getStatusCode());
		}
	}
	
	public void get(final String scalityId, final String filepath) throws Exception {
		final String url = getUrl(scalityId);
		final FileOutputStream fos = new FileOutputStream(filepath);
		log.info("Getting "+url+" to "+filepath);
		
		BoundRequestBuilder get = client.prepareGet(url);
		ListenableFuture<String> f = get.execute(new AsyncHandler<String>() {
			private Throwable t = null ;
			@Override
			public STATE onStatusReceived(HttpResponseStatus status) throws Exception {
				int statusCode = status.getStatusCode();
				if (statusCode != 200) {
					log.error("Get failed for Scality#"+scalityId+" with status code "+statusCode) ;
					return STATE.ABORT;
				}
				return STATE.CONTINUE;
			}

			@Override
			public STATE onHeadersReceived(HttpResponseHeaders h) throws Exception { 
				return STATE.CONTINUE; 
			}
			
			@Override
			public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
				fos.write(bodyPart.getBodyPartBytes());
				return STATE.CONTINUE;
			}

			@Override
			public String onCompleted() throws Exception {
				if(this.t != null)
					throw new Exception(t);
				fos.close();
				return filepath;
			}

			@Override
			public void onThrowable(Throwable t) {
				log.error("Get failed for Scality#"+scalityId, t);

			}
		});
		
		f.get();	
	}
	
	public void delete(String scalityId) throws Exception {
		final String url = getUrl(scalityId);
		log.trace("Deleting "+url);
		BoundRequestBuilder delete = client.prepareDelete(url);
		Response response = delete.execute().get();
		if (response.getStatusCode()!=200) {
			log.error("Response from Scality: "+response.getResponseBody());
			throw new Exception("Delete failed for Scality#"+scalityId+" with status code "+response.getStatusCode());
		}
		
	}
	
	public static String getUrl(String scalityId) {
		StringBuilder sb = new StringBuilder(SCALITY_BASE_URL);
		sb.append("/");
		sb.append(SCALITY_DRIVER);
		sb.append("/");
		sb.append(SCALITY_PATH_NAMESPACE);
		sb.append("/");
		sb.append(scalityId);
		return sb.toString();
	}
	
	public void close() {
		client.close();
	}
	
	public static void main(String[] args) throws Exception {
		
		ScalityDAO dao = new ScalityDAO();
		
		long start = System.currentTimeMillis();
		dao.put("1992585977687703650", "/Users/rokickik/Desktop/AlignedFlyBrainIntRescaled_signal.png");
//		dao.put("2020722762829529186", "/Users/rokickik/Desktop/GMR_OL0043B-20140611_32_A1_FLPO_20140620080403173_22068.lsm.bz2");
		dao.put("2020722763001495650", "/Users/rokickik/Desktop/GMR_OL0043B-20140611_32_A1_FLPO_20140620080415938_22069.lsm.bz2");
//		dao.put("1898944665143476231", "/Users/rokickik/Desktop/GMR_MB028B-20121018_19_A1~63x_stitched-1870576926020599906.v3draw");
		dao.put("1898937946963181575", "/Users/rokickik/Desktop/GMR_MB028B-20121018_19_A3~63x_stitched-1870576927434080354.v3draw");
		long stop = System.currentTimeMillis();
		log.info("Upload(s) took "+(stop-start)+" ms");		
		
//		start = System.currentTimeMillis();
//		dao.get("1841433294516781154", testFilepath+"-2");
//		stop = System.currentTimeMillis();
//		log.info("Download took "+(stop-start)+" ms");
//		
//		start = System.currentTimeMillis();
//		dao.delete("1841433294516781154");
//		stop = System.currentTimeMillis();
//		log.info("Deletion took "+(stop-start)+" ms");
		
		dao.close();
		System.exit(0);
	}
}
