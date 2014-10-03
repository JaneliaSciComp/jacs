package org.janelia.it.jacs.compute.access.scality;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.log4j.Logger;

public class ScalityDAO {
	
    private static final Logger log = Logger.getLogger(ScalityDAO.class);

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 1;
    private static final String SCALITY_PATH_NAMESPACE = "JACS";//SystemConfigurationProperties.getString("Scality.Namespace");
    private static final String SCALITY_BASE_URL = "http://s2-jrc:81/proxy";//SystemConfigurationProperties.getString("Scality.BaseURL");
	private static final String SCALITY_DRIVER = "bparc";//SystemConfigurationProperties.getString("Scality.Driver");
	
//	private AsyncHttpClient client;
	private HttpClient httpClient;
	
	public ScalityDAO() {
//		int timeout = 1000*60*60;
//		AsyncHttpClientConfig.Builder config = new AsyncHttpClientConfig.Builder();
//		config.setAllowPoolingConnections(true);
//		config.setPooledConnectionIdleTimeout(15000);
//		config.setMaxConnections(1000);
//		config.setMaxConnectionsPerHost(1000);
//		config.setRequestTimeout(timeout);
//		config.setConnectionTimeout(timeout);
//		config.setReadTimeout(timeout);
//		this.client = new DefaultAsyncHttpClient(config.build());
        MultiThreadedHttpConnectionManager mgr = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams managerParams = mgr.getParams();
        managerParams.setDefaultMaxConnectionsPerHost(2);
        managerParams.setMaxTotalConnections(20);
        this.httpClient = new HttpClient(mgr); 
	}
	
//	public void put(String scalityId, String filepath) throws Exception {
//		final String url = getUrl(scalityId);
//		log.info("Putting "+url+" from "+filepath);
//		BoundRequestBuilder put = client.preparePut(url);
//		put.setBody(new File(filepath));
//		Response response = put.execute().get();
//		
//		log.info("PUT status code: "+response.getStatusCode());
//		log.info("PUT response body: "+response.getResponseBody());
//		for (String key : response.getHeaders().keySet()) {
//			log.info(" "+key+": "+response.getHeaders().get(key));	
//		}
//		
//		if (response.getStatusCode()!=200) {
//			log.error("Response from Scality: "+response.getResponseBody());
//			throw new Exception("Put failed for Scality#"+scalityId+" with status code "+response.getStatusCode());
//		}
//	}

	public void put(String scalityId, String filepath) throws Exception {
		PutMethod put = null;
    	try {
			final String url = getUrl(scalityId);
			log.info("Putting "+url+" from "+filepath);
		
	        put = new PutMethod(url);
	
	        long start = System.currentTimeMillis();
	        
	        File f = new File(filepath);
//	        Part[] parts = {
//	            new StringPart("param_name", "value"),
//	            new FilePart(f.getName(), f)
//	        };
//	        put.setRequestEntity(new MultipartRequestEntity(parts, put.getParams()));
	        
	        FileRequestEntity re = new FileRequestEntity(f);
	    	put.setRequestEntity(re);
	        int responseCode = httpClient.executeMethod(put);

	        log.trace("Response code = "+responseCode);
	        log.trace("Response body = "+put.getResponseBodyAsString());
	        
	        if (responseCode!=HttpStatus.SC_OK) {
	        	throw new Exception("Put failed with code "+responseCode);
	        }
	        
	        long elapsed = System.currentTimeMillis() - start;
	        
			if (re.getBytesWritten()!=re.getContentLength()) {
				log.warn("Bytes written ("+re.getBytesWritten()+") is not what was expected ("+re.getContentLength()+")");
			}
			
			log.info("PUT "+re.getBytesWritten()+" bytes at "+getMbps(re.getBytesWritten(),elapsed)+" Mbps");
    	}
		finally {
			if (put!=null) put.releaseConnection();
		}
	}
	
	private class FileRequestEntity implements RequestEntity {

	    private File file = null;
	    private long count = 0;
	    
	    public FileRequestEntity(File file) {
	        super();
	        this.file = file;
	    }

	    public boolean isRepeatable() {
	        return true;
	    }

	    public String getContentType() {
	        return "application/octet-stream";
	    }
	    
	    public void writeRequest(OutputStream out) throws IOException {
	        InputStream in = new FileInputStream(this.file);
	        try {
	            int l;
	            byte[] buffer = new byte[1024];
	            while ((l = in.read(buffer)) != -1) {
	                out.write(buffer, 0, l);
	                count += l;
	            }
	        } finally {
	            in.close();
	        }
	    }

	    public long getContentLength() {
	        return file.length();
	    }
	    
	    public long getBytesWritten() {
	    	return count;
	    }
	}
	
//	public void get(final String scalityId, final String filepath) throws Exception {
//		final String url = getUrl(scalityId);
//		final FileOutputStream fos = new FileOutputStream(filepath);
//		log.info("Getting "+url+" to "+filepath);
//		
//		BoundRequestBuilder get = client.prepareGet(url);
//		ListenableFuture<String> f = get.execute(new AsyncHandler<String>() {
//			private Throwable t = null ;
//			@Override
//			public STATE onStatusReceived(HttpResponseStatus status) throws Exception {
//				int statusCode = status.getStatusCode();
//				if (statusCode != 200) {
//					log.error("Get failed for Scality#"+scalityId+" with status code "+statusCode) ;
//					return STATE.ABORT;
//				}
//				return STATE.CONTINUE;
//			}
//
//			@Override
//			public STATE onHeadersReceived(HttpResponseHeaders h) throws Exception { 
//				return STATE.CONTINUE; 
//			}
//			
//			@Override
//			public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
//				fos.write(bodyPart.getBodyPartBytes());
//				return STATE.CONTINUE;
//			}
//
//			@Override
//			public String onCompleted() throws Exception {
//				if(this.t != null)
//					throw new Exception(t);
//				fos.close();
//				return filepath;
//			}
//
//			@Override
//			public void onThrowable(Throwable t) {
//				log.error("Get failed for Scality#"+scalityId, t);
//
//			}
//		});
//		
//		f.get();	
//	}

	public void get(final String scalityId, final String filepath) throws Exception {
		get(scalityId, filepath, DEFAULT_BUFFER_SIZE);
	}
	
	public void get(final String scalityId, final String filepath, int bufferSize) throws Exception {
    	GetMethod get = null;
    	try {
    		String url = ScalityDAO.getUrl(scalityId);
    		log.info("Getting "+url+" to "+filepath);
    		
            get = new GetMethod(url);
            
            long start = System.currentTimeMillis();
            
            int responseCode = httpClient.executeMethod(get);

            log.trace("Response code = "+responseCode);
            String contentLengthStr = get.getResponseHeader("Content-length").getValue();
            long contentLength = Long.parseLong(contentLengthStr);
            log.trace("Response length = "+contentLength);

            if (responseCode!=HttpStatus.SC_OK) {
            	throw new Exception("Get failed with code "+responseCode);
            }
            
            FileOutputStream fos = new FileOutputStream(new File(filepath));
            copyBytes(get.getResponseBodyAsStream(), fos, contentLength, bufferSize);
            fos.close();
            
            long elapsed = System.currentTimeMillis() - start;
    		log.info("GET "+contentLength+" bytes at "+getMbps(contentLength,elapsed)+" Mbps");
    	}
    	finally {
			if (get!=null) get.releaseConnection();
		}
    }
	
//	public void delete(String scalityId) throws Exception {
//		final String url = getUrl(scalityId);
//		log.trace("Deleting "+url);
//		BoundRequestBuilder delete = client.prepareDelete(url);
//		Response response = delete.execute().get();
//		if (response.getStatusCode()!=200) {
//			log.error("Response from Scality: "+response.getResponseBody());
//			throw new Exception("Delete failed for Scality#"+scalityId+" with status code "+response.getStatusCode());
//		}
//		
//	}

	public void delete(String scalityId) throws Exception {

		DeleteMethod delete = null;
		try {
			final String url = getUrl(scalityId);
			log.info("Deleting "+url);
		
			delete = new DeleteMethod(url);
	        int responseCode = httpClient.executeMethod(delete);
	        
	        log.trace("Response code = "+responseCode);
	        log.trace("Response body = "+delete.getResponseBodyAsString());
	        
	        if (responseCode!=HttpStatus.SC_OK) {
	        	throw new Exception("Delete failed with code "+responseCode);
	        }
		}
		finally {
			if (delete!=null) delete.releaseConnection();
		}
	}
	
	public void close() {
//		if (client!=null) client.close();
	}

	private static long copyBytes(InputStream input, OutputStream output, long length, int bufferSize) throws IOException {
		byte[] buffer = new byte[bufferSize];
		long count = 0;
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			if (count>length) {
				n = (int)(count - length);
				log.warn("Got more data than content size. Truncating to "+length+".");
				output.write(buffer, 0, n);
				count += n;
				return count;
			}
			output.write(buffer, 0, n);
			count += n;
		}
		if (count!=length) {
			log.warn("Bytes written ("+count+") is not what was expected ("+length+")");
		}
		return count;
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

	public static long getMbps(long bytes, long millis) {
		return getKbps(bytes, millis) / 1000;
	}
	
	public static long getKbps(long bytes, long millis) {
		return (bytes*8) / (millis/1000) / 1000;
	}
	
	public static void main(String[] args) throws Exception {
		
		ScalityDAO dao = new ScalityDAO();
		
		long elapsed, start;
		
//		dao.put2("1870576927434080356", "/home/rokickik/stitched-1870576927434080354.v3dpbd");
//		dao.put2("1870576927434080357", "/home/rokickik/stitched-1947307635921387618.v3dpbd");
//		dao.put("1870576927434080356", "/home/rokickik/stitched-1870576927434080354.v3dpbd");
		dao.put("1870576927434080357", "/home/rokickik/stitched-1947307635921387618.v3dpbd");
//		
//		dao.get("1870576927434080356", "/home/rokickik/stitched-1870576927434080354.v3dpbd2",1024 * 1);
		dao.get("1870576927434080357", "/home/rokickik/stitched-1947307635921387618.v3dpbd2",1024 * 1);
//		dao.get2("1870576927434080356", "/home/rokickik/stitched-1870576927434080354.v3dpbd3",1024 * 2);
//		dao.get2("1870576927434080357", "/home/rokickik/stitched-1947307635921387618.v3dpbd3",1024 * 2);
//		dao.get2("1870576927434080356", "/home/rokickik/stitched-1870576927434080354.v3dpbd4",1024 * 4);
//		dao.get2("1870576927434080357", "/home/rokickik/stitched-1947307635921387618.v3dpbd4",1024 * 4);
	
		dao.delete("1870576927434080357");
		
//		13:00:32.508 [main] INFO  org.janelia.it.jacs.compute.access.scality.ScalityDAO  - PUT 2772074402 bytes at 693 Mbps
//		13:03:09.861 [main] INFO  org.janelia.it.jacs.compute.access.scality.ScalityDAO  - PUT 9858181219 bytes at 502 Mbps
//		13:03:48.545 [main] INFO  org.janelia.it.jacs.compute.access.scality.ScalityDAO  - PUT 2772074402 bytes at 583 Mbps
//		13:06:09.485 [main] INFO  org.janelia.it.jacs.compute.access.scality.ScalityDAO  - PUT 9858181219 bytes at 563 Mbps
		
//		13:07:07.160 [main] INFO  org.janelia.it.jacs.compute.access.scality.ScalityDAO  - GET 2772074402 bytes at 389 Mbps
//		13:10:20.134 [main] INFO  org.janelia.it.jacs.compute.access.scality.ScalityDAO  - GET 9858181219 bytes at 410 Mbps
//		13:11:18.462 [main] INFO  org.janelia.it.jacs.compute.access.scality.ScalityDAO  - GET 2772074402 bytes at 382 Mbps
//		13:14:26.184 [main] INFO  org.janelia.it.jacs.compute.access.scality.ScalityDAO  - GET 9858181219 bytes at 421 Mbps
//		13:15:23.940 [main] INFO  org.janelia.it.jacs.compute.access.scality.ScalityDAO  - GET 2772074402 bytes at 389 Mbps
//		13:18:16.503 [main] INFO  org.janelia.it.jacs.compute.access.scality.ScalityDAO  - GET 9858181219 bytes at 458 Mbps

//		13:23:56.467 [main] INFO  org.janelia.it.jacs.compute.access.scality.ScalityDAO  - PUT 2772074402 bytes at 462 Mbps
//		13:26:37.255 [main] INFO  org.janelia.it.jacs.compute.access.scality.ScalityDAO  - PUT 9858181219 bytes at 492 Mbps
//		13:27:08.993 [main] INFO  org.janelia.it.jacs.compute.access.scality.ScalityDAO  - PUT 2772074402 bytes at 715 Mbps
//		13:29:06.780 [main] INFO  org.janelia.it.jacs.compute.access.scality.ScalityDAO  - PUT 9858181219 bytes at 674 Mbps
		
//		13:30:05.955 [main] INFO  org.janelia.it.jacs.compute.access.scality.ScalityDAO  - GET 2772074402 bytes at 375 Mbps
//		13:33:16.744 [main] INFO  org.janelia.it.jacs.compute.access.scality.ScalityDAO  - GET 9858181219 bytes at 415 Mbps
//		13:34:15.412 [main] INFO  org.janelia.it.jacs.compute.access.scality.ScalityDAO  - GET 2772074402 bytes at 382 Mbps
//		13:37:21.267 [main] INFO  org.janelia.it.jacs.compute.access.scality.ScalityDAO  - GET 9858181219 bytes at 426 Mbps
//		13:38:19.643 [main] INFO  org.janelia.it.jacs.compute.access.scality.ScalityDAO  - GET 2772074402 bytes at 382 Mbps
//		13:41:31.766 [main] INFO  org.janelia.it.jacs.compute.access.scality.ScalityDAO  - GET 9858181219 bytes at 410 Mbps

		
//		File tmpDir = new File("/tmp");
//		File destDir = new File("/nobackup/jacs/jacsData/filestore/rokickik/Test");
//		
//		File file1 = new File("/home/rokickik/stitched-1870576927434080354.v3dpbd");
//		File file2 = new File("/home/rokickik/stitched-1947307635921387618.v3dpbd");
//		
//		start = System.currentTimeMillis();
//		File file1t = new File(destDir, file1.getName());
//		FileUtil.copyFile(file1, file1t);
//		elapsed = System.currentTimeMillis()-start;
//		log.info("PUT "+file1.length()+" bytes at "+getMbps(file1.length(),elapsed)+" Mbps");
//
//		start = System.currentTimeMillis();
//		File file2t = new File(destDir, file2.getName());
//		FileUtil.copyFile(file2, file2t);
//		elapsed = System.currentTimeMillis()-start;
//		log.info("PUT "+file2.length()+" bytes at "+getMbps(file2.length(),elapsed)+" Mbps");
//		
//		start = System.currentTimeMillis();
//		File file1s = new File(tmpDir, "file1");
//		FileUtil.copyFile(file1, file1s);
//		elapsed = System.currentTimeMillis()-start;
//		log.info("GET "+file1s.length()+" bytes at "+getMbps(file1s.length(),elapsed)+" Mbps");
//
//		start = System.currentTimeMillis();
//		File file2s = new File(tmpDir, "file2");
//		FileUtil.copyFile(file2, file2s);
//		elapsed = System.currentTimeMillis()-start;
//		log.info("GET "+file2s.length()+" bytes at "+getMbps(file2s.length(),elapsed)+" Mbps");
		
		
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
