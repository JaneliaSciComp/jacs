package org.janelia.it.jacs.compute.wsrest;

import java.util.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.internal.util.Base64;
import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.shared.security.LDAPProvider;
import org.janelia.it.jacs.shared.security.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.user_data.UserToolEvent;
import org.janelia.it.jacs.shared.security.BasicAuthToken;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.janelia.it.jacs.shared.utils.DomainQuery;


@Path("/")
public class UserWorkstationWebService extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(DataViewsWebService.class);

    @Context
    SecurityContext securityContext;

    @Context
    HttpHeaders headers;

    LDAPProvider authenticator;

    public UserWorkstationWebService() {
        register(JacksonFeature.class);
        authenticator = new LDAPProvider();
    }

    @GET
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON)
    public Subject loginSubject (@QueryParam("subjectKey") String subjectKey) {
        // user authentication
        Token userInfo = getCredentials();
        if (authenticator.login(userInfo)) {
            // once user authenticated, check the runAsUser; if not the same as username, make sure they are admin

            // check subjects, if not subject exists for this user and they are in jacsdata, create the account
        }
        return null;
    }

    // provides endpoints to programmatically manage session information
    // this would normally be handled automatically by RESTful service
    public void beginSession() {
        // set up session information (log into Mongo)
    }

    public void addEventToSession(UserToolEvent event) {
    }

    public void endSession() {
    }

    // mapping using explicit object mapping; TO DO configure jackson integration with jersey
    @GET
    @Path("/workspace")
    @Produces(MediaType.APPLICATION_JSON)
    public Workspace getWorkspace(@QueryParam("subjectKey") String subjectKey) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            return dao.getDefaultWorkspace(subjectKey);
        } catch (Exception e) {
            log.error("Error occurred getting default workspace" + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/workspaces")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Workspace> getAllWorkspace(@QueryParam("subjectKey") String subjectKey) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            return new ArrayList<Workspace>(dao.getWorkspaces(subjectKey));
        } catch (Exception e) {
            log.error("Error occurred getting default workspace" + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @PUT
    @Path("/treenode")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TreeNode createTreeNode(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            return (TreeNode)dao.save(query.getSubjectKey(), query.getDomainObject());
        } catch (Exception e) {
            log.error("Error occurred creating tree node" + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @Path("/treenode/reorder")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TreeNode reorderTreeNode(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();

        try {
            List<Integer> orderList = query.getOrdering();
            int[] order = new int[orderList.size()];
            for (int i=0; i<orderList.size(); i++) {
                order[i] = orderList.get(i).intValue();
            }
            return dao.reorderChildren(query.getSubjectKey(), (TreeNode) query.getDomainObject(), order);
        } catch (Exception e) {
            log.error("Error occurred reordering Tree Node" + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @PUT
    @Path("/treenode/children")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TreeNode addChildren(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            return dao.addChildren(query.getSubjectKey(), (TreeNode) query.getDomainObject(), query.getReferences());
        } catch (Exception e) {
            log.error("Error occurred add children to tree node " + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }


    @POST
    @Path("/treenode/children")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TreeNode removeChildren(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            return dao.removeChildren(query.getSubjectKey(), (TreeNode) query.getDomainObject(), query.getReferences());
        } catch (Exception e) {
            log.error("Error occurred removing children from tree node " + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }


    @PUT
    @Path("/objectset")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ObjectSet createObjectSet(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            return (ObjectSet)dao.save(query.getSubjectKey(), query.getDomainObject());
        } catch (Exception e) {
            log.error("Error occurred creating object set" + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @PUT
    @Path("/objectset/member")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ObjectSet addMembers(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            return dao.addMembers(query.getSubjectKey(), (ObjectSet) query.getDomainObject(), query.getReferences());
        } catch (Exception e) {
            log.error("Error occurred adding members to object set" + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @Path("/objectset/member")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ObjectSet removeMembers(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            return dao.removeMembers(query.getSubjectKey(), (ObjectSet) query.getDomainObject(), query.getReferences());
        } catch (Exception e) {
            log.error("Error occurred removing members from Object Set" + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/user/subjects")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<Subject> getSubjects() {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            return dao.getSubjects();
        }
        catch (Exception e) {
            log.error("Error occurred getting subjects" + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }


    @GET
    @Path("/user/subject")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Subject getSubjectByKey(@QueryParam("subjectKey") String subjectKey) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            return dao.getSubjectByKey(subjectKey);
        }
        catch (Exception e) {
            log.error("Error occurred finding subject " + subjectKey + ", " + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/user/preferences")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<Preference> getPreferences(@QueryParam("subjectKey") String subjectKey) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            return dao.getPreferences(subjectKey);
        }
        catch (Exception e) {
            log.error("Error occurred getting preferences" + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @PUT
    @Path("/user/preferences")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Preference setPreferences(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            return dao.save(query.getSubjectKey(), query.getPreference());
        }
        catch (Exception e) {
            log.error("Error occurred setting preferences" + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @PUT
    @Path("/user/permissions")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void changePermissions(Map<String, Object> params) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            DomainObject domainObj = (DomainObject)params.get("target");
            dao.changePermissions((String) params.get("subjectKey"), (String) params.get("targetClass"), (Long)params.get("targetId"),
                    (String) params.get("granteeKey"), (String) params.get("rights"), ((Boolean) params.get("grant")).booleanValue());
        }
        catch (Exception e) {
            log.error("Error occurred setting permissions" + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }


    private Token getCredentials() {
        List<String> authHeaders = headers.getRequestHeader(HttpHeaders.AUTHORIZATION);
        // if no basic auth, return error
        if (authHeaders == null || authHeaders.size() < 1) {
            throw new RuntimeException("Not using basic authentication to login");
        }
        String encodedBasicAuth = authHeaders.get(0);
        String base64Credentials = encodedBasicAuth.substring("Basic".length()).trim();
        String credentials = new String(Base64.decodeAsString(base64Credentials));
        BasicAuthToken token = new BasicAuthToken();
        String[] creds = credentials.split(":",2);
        token.setUsername(creds[0]);
        token.setPassword(creds[1]);
        return token;
    }

}