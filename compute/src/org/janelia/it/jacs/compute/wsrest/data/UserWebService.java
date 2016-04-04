package org.janelia.it.jacs.compute.wsrest.data;

import java.util.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponses;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.internal.util.Base64;
import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.compute.wsrest.WebServiceContext;
import org.janelia.it.jacs.shared.security.LDAPProvider;
import org.janelia.it.jacs.shared.security.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.user_data.UserToolEvent;
import org.janelia.it.jacs.shared.security.BasicAuthToken;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.shared.utils.DomainQuery;


@Path("/data")
@Api(value = "Janelia Workstation", description = "Services for managing data in the workstation")
public class UserWebService extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(UserWebService.class);

    @Context
    SecurityContext securityContext;

    @Context
    HttpHeaders headers;

    LDAPProvider authenticator;

    public UserWebService() {
        register(JacksonFeature.class);
        authenticator = new LDAPProvider();
        authenticator.init();
    }

    @GET
    @Path("/login")
    @ApiOperation(value = "Logs a User in and generates a JSON Web Token",
            notes = "")
    @ApiResponses(value = {

    })
    @Produces(MediaType.APPLICATION_JSON)
    public Subject loginSubject () {
        // user authentication
        BasicAuthToken userInfo = (BasicAuthToken)getCredentials();
        if (authenticator.login(userInfo)) {
            // check subjects, if subject doesn't exist for this user and they are in jacsdata,
            // create the account
            DomainDAO dao = WebServiceContext.getDomainManager();
            try {
                Subject user = dao.getSubjectByKey("user:" + userInfo.getUsername());
                if (user==null) {
                    // create a general workstation account for this user since they authenticate against LDAP
                    Subject newUser = authenticator.generateSubjectInfo(userInfo.getUsername());
                    return dao.save(newUser);
                }
                return user;
            } catch (Exception e) {
                log.error("Error occurred authenticating user",e);
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            }
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

    @GET
    @Path("/user/subjects")
    @ApiOperation(value = "Gets a List of the Users in the Workstation",
            notes = "")
    @ApiResponses(value = {

    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<Subject> getSubjects() {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            log.trace("getSubjects()");
            return dao.getSubjects();
        }
        catch (Exception e) {
            log.error("Error occurred getting subjects",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }


    @GET
    @Path("/user/subject")
    @ApiOperation(value = "Returns a Subject Object by Key",
            notes = "")
    @ApiResponses(value = {

    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Subject getSubjectByKey(@QueryParam("subjectKey") String subjectKey) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            log.debug("getSubjectByKey({})",subjectKey);
            return dao.getSubjectByKey(subjectKey);
        }
        catch (Exception e) {
            log.error("Error occurred finding subject " + subjectKey,e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/user/preferences")
    @ApiOperation(value = "Get a User's Preferences",
            notes = "")
    @ApiResponses(value = {

    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<Preference> getPreferences(@QueryParam("subjectKey") String subjectKey) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            log.debug("getPreferences({})",subjectKey);
            return dao.getPreferences(subjectKey);
        }
        catch (Exception e) {
            log.error("Error occurred getting preferences",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @PUT
    @Path("/user/preferences")
    @ApiOperation(value = "Sets a User's Preferences",
            notes = "")
    @ApiResponses(value = {

    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Preference setPreferences(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            log.debug("setPreferences({},{})",query.getSubjectKey(),query.getPreference());
            return dao.save(query.getSubjectKey(), query.getPreference());
        }
        catch (Exception e) {
            log.error("Error occurred setting preferences",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @PUT
    @Path("/user/permissions")
    @ApiOperation(value = "Change the permissions of a user",
            notes = "")
    @ApiResponses(value = {

    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void changePermissions(Map<String, Object> params) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            log.debug("changePermissions({})",params);
            dao.changePermissions((String) params.get("subjectKey"), (String) params.get("targetClass"), (Long)params.get("targetId"),
                    (String) params.get("granteeKey"), (String) params.get("rights"), ((Boolean) params.get("grant")).booleanValue());
        }
        catch (Exception e) {
            log.error("Error occurred setting permissions",e);
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