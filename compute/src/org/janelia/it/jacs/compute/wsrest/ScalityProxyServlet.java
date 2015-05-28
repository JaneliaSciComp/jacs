package org.janelia.it.jacs.compute.wsrest;

import java.io.IOException;

import javax.servlet.ServletContext;
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

public class ScalityProxyServlet extends HttpServlet {

	private Logger log = Logger.getLogger(ScalityProxyServlet.class);
	
    private HttpClient httpClient;
    
	@Override
	public void init() {
		log.info("Init proxy servlet");
        MultiThreadedHttpConnectionManager mgr = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams managerParams = mgr.getParams();
        managerParams.setDefaultMaxConnectionsPerHost(2);
        managerParams.setMaxTotalConnections(20);
        this.httpClient = new HttpClient(mgr);
    }
	
	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		String entityIdParam = request.getParameter("id");
		ServletContext context  = getServletConfig().getServletContext();
		
		try {
//			Long entityId = Long.parseLong(entityIdParam);
//	        Entity entity = EJBFactory.getLocalEntityBean().getEntityById(null, entityId);
//	        if (entity==null) {
//	        	log.error("Entity not found with id="+entityId);
//	        	response.setStatus(404);
//	        	return;
//	        }
//	        
//			log.info("GET "+entityId+" "+entity.getName());
			
			String filename = entityIdParam+".bin";
	        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
	        
	        Long entityId = Long.parseLong(entityIdParam);
    		String url = ScalityDAO.getUrlFromEntityId(entityId);

    		log.info(entityIdParam+" "+url);
    		
    		// TODO: use ScalityDAO instead
    		
            GetMethod get = new GetMethod(url);
            int responseCode = httpClient.executeMethod(get);

            log.info(entityIdParam+" responseCode="+responseCode);
            
            if (responseCode!=HttpServletResponse.SC_OK) {
            	response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            	log.error("Got no OK response code from sproxyd: "+responseCode);
            	return;
            }
            
            Header contentLengthHeader = get.getResponseHeader("Content-Length");
            if (contentLengthHeader!=null) {
            	response.setHeader("Content-Length", contentLengthHeader.getValue());
                log.info(entityIdParam+" length="+contentLengthHeader.getValue());
            }
            
            log.info(entityIdParam+" begin streaming");
            IOUtils.copy(get.getResponseBodyAsStream(), response.getOutputStream());
            log.info(entityIdParam+" done streaming");
		}
		catch (Exception e) {
			throw new ServletException("Error proxying file", e);
		}
    }

}
