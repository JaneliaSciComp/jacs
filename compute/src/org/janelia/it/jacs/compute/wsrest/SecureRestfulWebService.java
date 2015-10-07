package org.janelia.it.jacs.compute.wsrest;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanRemote;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.json.JsonEntity;
import org.janelia.it.jacs.model.status.RestfulWebServiceFailure;
import org.jboss.resteasy.annotations.providers.jaxb.Formatted;
import org.jboss.resteasy.annotations.providers.jaxb.Wrapped;
import org.jboss.resteasy.spi.UnauthorizedException;

/**
 * Defines the secure RESTful web service entry points.
 *
 * <p>
 * The web-rest-ws.xml file references the resteasy components that are
 * used to load an instance of this class as a JAX-RS service.
 * </p>
 * <p>
 * The application.xml file defines the root context for this service.
 * A root context of "/secure/rest-v1" would make resources available at: <br/>
 * http://[compute-server]:8180/secure/rest-v1/[annotated path]
 * (e.g. http://jacs:8180/secure/rest-v1/dataSet ).
 * </p>
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Path("/")
public class SecureRestfulWebService {

    private Logger log = Logger.getLogger(this.getClass());
    
    @Context
    SecurityContext securityContext;
    
    /**
     * Retrieve the authenticated subject's workspaces.
     *
     * @return list of workspace entities
     */
    @GET
//    @PermitAll
    @Path("workspace")
    @Produces("application/json")
    @Formatted
    public Response getWorkspace(@Context UriInfo uriInfo) {
    	return getWorkspace(null, uriInfo);
    }

    @GET
//    @PermitAll
    @Path("workspace/{workspaceId}")
    @Produces("application/json")
    @Formatted
    @Wrapped(element = "workspace")
    public Response getWorkspace(@PathParam("workspaceId") Long workspaceId, @Context UriInfo uriInfo) {
    	Response response = null;
    	String subjectKey = getSubjectKey();
        String context = "getWorkspace: ";
        try {
        	log.info("getWorkspace called by "+subjectKey);
            EntityBeanRemote entityBean = EJBFactory.getRemoteEntityBean();
            if (workspaceId==null) {
            	List<Entity> workspaces = entityBean.getWorkspaces(subjectKey);
            	response = Response.status(Response.Status.OK).entity(getJsonEntityList(workspaces)).build();
            }
            else {
            	Entity workspace = entityBean.getEntityById(subjectKey, workspaceId);
            	if (workspace==null) {
            		response = Response.status(Response.Status.NOT_FOUND).build();
            	}
            	else {
                	if (!EntityConstants.TYPE_WORKSPACE.equals(workspace.getEntityTypeName())) {
                		response = Response.status(Response.Status.NOT_FOUND).build();
                	}
                	else {
                		response = Response.status(Response.Status.OK).entity(new JsonEntity(workspace)).build();	
                	}
            	}
            }
        } 
        catch (Exception e) {
            response = getErrorResponse(context, Response.Status.INTERNAL_SERVER_ERROR, "failed to run", e);
        }
        return response;
    }

    @GET
//    @PermitAll
    @Path("entity/{entityId}")
    @Produces("application/json")
    @Formatted
    public Response getEntity(@PathParam("entityId") Long entityId, @Context UriInfo uriInfo) {
    	Response response = null;
    	String subjectKey = getSubjectKey();
        String context = "getEntity: ";
        try {
        	log.info("getEntity called by "+subjectKey);
            EntityBeanRemote entityBean = EJBFactory.getRemoteEntityBean();
        	Entity workspace = entityBean.getEntityById(subjectKey, entityId);
        	if (workspace==null) {
        		response = Response.status(Response.Status.NOT_FOUND).build();
        	}
        	else {
        		response = Response.status(Response.Status.OK).entity(new JsonEntity(workspace)).build();
        	}
        } 
        catch (Exception e) {
            response = getErrorResponse(context, Response.Status.INTERNAL_SERVER_ERROR, "failed to run", e);
        }
        return response;
    }
    
    private List<JsonEntity> getJsonEntityList(List<Entity> entityList) {
    	List<JsonEntity> jsonList = new ArrayList<JsonEntity>();
    	for(Entity entity : entityList) {
    		jsonList.add(new JsonEntity(entity));
    	}
    	return jsonList;
    }

//    @GET
//    @Path("file/{entityId}")
//	@Produces(MediaType.APPLICATION_OCTET_STREAM)
//	public Response getFile(@PathParam("entityId") Long entityId, @Context UriInfo uriInfo) throws Exception {
//
//        String context = "getTest: ";
//
//		AsyncHttpClientConfig.Builder config = new AsyncHttpClientConfig.Builder();
//		config.setAllowPoolingConnections(true);
//		config.setPooledConnectionIdleTimeout(15000);
//		config.setMaxConnections(1000);
//		config.setMaxConnectionsPerHost(1000);
//		final AsyncHttpClient httpClient = new DefaultAsyncHttpClient(config.build());
//		
//    	ScalityDAO scality = new ScalityDAO();
//    	AsyncStreamCoupler coupler = new AsyncStreamCoupler(httpClient);
//    	try {
//    		scality.get(coupler, entityId.toString());
//		}
//		catch (Exception e) {
//			return getErrorResponse(context, Response.Status.INTERNAL_SERVER_ERROR, "Error getting async stream coupler", e);
//		}
//    	
//    	final ServletStreamCoupler finalCoupler = coupler;
//        
//		StreamingOutput stream = new StreamingOutput() {
//			@Override
//			public void write(OutputStream output) throws IOException, WebApplicationException {
//				try {
//					finalCoupler.writeTo(output);
//				} 
//				catch (Exception e) {
//					throw new WebApplicationException(e);
//				}
//			}
//		};
//		
//		String contentLength = null;
//		try {
//			contentLength = coupler.getContentLength().get();
//			return Response
//					.ok(stream, MediaType.APPLICATION_OCTET_STREAM)
//					.header("content-disposition",
//							"attachment; filename=\"temp.bin\"")
//					.header("Content-Length", contentLength).build();
//		}
//		catch (Exception e) {
//			return getErrorResponse(context, Response.Status.INTERNAL_SERVER_ERROR, "Error getting content length header", e);
//		}
//	}
	
    private String getNormalizedBaseUrlString(UriInfo uriInfo) {
        StringBuilder sb = new StringBuilder(uriInfo.getBaseUri().toString());
        if (sb.charAt(sb.length() - 1) != '/') {
            sb.append('/');
        }
        return sb.toString();
    }

    private Response getErrorResponse(String context, Response.Status status, String errorMessage)  {
    	return getErrorResponse(context, status, errorMessage, null);
    }
    
    private Response getErrorResponse(String context, Response.Status status, String errorMessage, Exception e)  {
        final RestfulWebServiceFailure failure = new RestfulWebServiceFailure(errorMessage, e);
        if (e != null) {
        	log.error(context + errorMessage, e);
        }
        return Response.status(status).entity(failure).build();
    }

    private String getResponseString(Response response) {
        return response.getStatus() + ": " + response.getEntity();
    }
    
    private String getSubjectKey() {
        if (securityContext==null || securityContext.getUserPrincipal() == null)
            throw new UnauthorizedException("User has no security context");
        return securityContext.getUserPrincipal().getName();
    }
}
