package org.janelia.it.jacs.compute.wsrest;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.scality.ScalityDAO;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.entity.Entity;

/**
 * Proxy servlet which fetches objects from Scality based on an Entity's BPID.
 * 
 * Usage looks something like this:
 * http://jacs:8180/compute/ScalityProxy?id=2143188828775514283&username=group:dicksonlab
 *
 * The entity given by the id must be accessible by the given user, and it must have a Scality BPID.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ScalityProxyServlet extends HttpServlet {

	private Logger log = Logger.getLogger(ScalityProxyServlet.class);

    private HttpClient httpClient;
    
	@Override
	public void init() {
        MultiThreadedHttpConnectionManager mgr = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams managerParams = mgr.getParams();
        managerParams.setDefaultMaxConnectionsPerHost(2);
        managerParams.setMaxTotalConnections(20);
        this.httpClient = new HttpClient(mgr);
    }
	
	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

	    ///compute/ScalityProxy/JACS/2143188828775514283/test.v3dpbd
	    
	    String path = request.getPathInfo();
        String bpid = null;
        String filename = null;

	    Pattern p = Pattern.compile("/(.*?)/([^/]*?)");
	    Matcher m = p.matcher(path);
        if (m.matches()) {
            bpid = m.group(1);
            filename = m.group(2);
        }
        else {
            response.setStatus(400);
            return;
        }
        
        // TODO: get from BASIC auth
        String username = null;
	    log.info("principle: "+request.getUserPrincipal());
	    
		try {
		    if (bpid==null) {
                response.setStatus(400);
                return;
		    }

		    Long entityId = ScalityDAO.getEntityIdFromBPID(bpid);
		    if (entityId==null) {
                response.setStatus(400);
                return;
		    }
		    
	        Entity entity = EJBFactory.getLocalEntityBean().getEntityById(username, entityId);
	        if (entity==null) {
	        	log.warn("Entity not found with id="+entityId+" for user "+username);
	        	response.setStatus(404);
	        	return;
	        }
	        
    		String url = ScalityDAO.getUrlFromBPID(bpid);
    		log.info("Proxying "+url);

    		if (filename==null) {
                filename = entity.getName();
                response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
    		}
    		
            GetMethod get = new GetMethod(url);
            int responseCode = httpClient.executeMethod(get);

            log.trace(bpid+" responseCode="+responseCode);
            
            if (responseCode!=HttpServletResponse.SC_OK) {
            	response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            	log.error("Got not OK response code from Scality: "+responseCode);
            	return;
            }
            
            Header contentLengthHeader = get.getResponseHeader("Content-Length");
            if (contentLengthHeader!=null) {
            	response.setHeader("Content-Length", contentLengthHeader.getValue());
                log.trace(bpid+" length="+contentLengthHeader.getValue());
            }
            
            log.trace(bpid+" begin streaming");
            IOUtils.copy(get.getResponseBodyAsStream(), response.getOutputStream());
            log.trace(bpid+" done streaming");
		}
		catch (Exception e) {
			throw new ServletException("Error proxying Scality object with BPID "+
			        bpid+" and username "+username, e);
		}
    }
}
