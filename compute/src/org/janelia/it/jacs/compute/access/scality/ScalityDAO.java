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

/**
 * DAO for CRUD operations against the Scality key store. 
 *  
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ScalityDAO {
	
    private static final Logger log = Logger.getLogger(ScalityDAO.class);

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 2;
    private static final String SCALITY_PATH_NAMESPACE = "JACS";//SystemConfigurationProperties.getString("Scality.Namespace");
    private static final String SCALITY_BASE_URL = "http://sc101-jrc:81/proxy";//SystemConfigurationProperties.getString("Scality.BaseURL");
	private static final String SCALITY_DRIVER = "bparc2";//SystemConfigurationProperties.getString("Scality.Driver");
	
	private HttpClient httpClient;
	
	public ScalityDAO() {
        MultiThreadedHttpConnectionManager mgr = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams managerParams = mgr.getParams();
        managerParams.setDefaultMaxConnectionsPerHost(2);
        managerParams.setMaxTotalConnections(20);
        this.httpClient = new HttpClient(mgr); 
	}
	
	public void put(Long entityId, String filepath) throws Exception {
		PutMethod put = null;
    	try {
			final String url = getUrlFromEntityId(entityId);
			log.info("Putting "+url+" from "+filepath);
		
	        put = new PutMethod(url);
	
	        long start = System.currentTimeMillis();
	        
	        File f = new File(filepath);
	        
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
	
	public void get(final Long entityId, final String filepath) throws Exception {
		get(entityId, filepath, DEFAULT_BUFFER_SIZE);
	}
	
	public void get(final Long entityId, final String filepath, int bufferSize) throws Exception {
    	GetMethod get = null;
    	try {
    		String url = ScalityDAO.getUrlFromEntityId(entityId);
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
	
	public void delete(Long entityId) throws Exception {

		DeleteMethod delete = null;
		try {
			final String url = getUrlFromEntityId(entityId);
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

    public static String getBPIDFromEntityId(Long entityId) {
        StringBuilder sb = new StringBuilder(SCALITY_PATH_NAMESPACE);
        sb.append("/");
        sb.append(entityId);
        return sb.toString();
    }

	public static String getUrlFromBPID(String bpid) {
		StringBuilder sb = new StringBuilder(SCALITY_BASE_URL);
		sb.append("/");
		sb.append(SCALITY_DRIVER);
		sb.append("/");
		sb.append(bpid);
		return sb.toString();
	}

    public static String getUrlFromEntityId(Long entityId) {
        return getUrlFromBPID(getBPIDFromEntityId(entityId));
    }
    
	private static long getMbps(long bytes, long millis) {
		return getKbps(bytes, millis) / 1000;
	}
	
	private static long getKbps(long bytes, long millis) {
		return Math.round(((double)bytes*8) / ((double)millis/1000) / 1000);
	}
	
	public static void main(String[] args) throws Exception {
		
		ScalityDAO dao = new ScalityDAO();
		
		dao.put(1904834176872349794L, "/Users/rokickik/1904834176872349794.v3dpbd");
        dao.put(2141686516697530539L, "/Users/rokickik/2141686516697530539.v3dpbd");

        dao.get(1904834176872349794L, "/Users/rokickik/1904834176872349794-2.v3dpbd");
		dao.get(2141686516697530539L, "/Users/rokickik/2141686516697530539-2.v3dpbd");

        dao.delete(1904834176872349794L);
		dao.delete(2141686516697530539L);
		
		dao.close();
		System.exit(0);
	}
}
