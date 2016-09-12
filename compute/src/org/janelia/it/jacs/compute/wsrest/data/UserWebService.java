package org.janelia.it.jacs.compute.wsrest.data;

import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang.time.StopWatch;
import org.glassfish.jersey.internal.util.Base64;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.compute.access.domain.DomainDAL;
import org.janelia.it.jacs.compute.util.ActivityLogHelper;
import org.janelia.it.jacs.compute.wsrest.security.BasicAuthToken;
import org.janelia.it.jacs.compute.wsrest.security.LDAPProvider;
import org.janelia.it.jacs.compute.wsrest.security.Token;
import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.user_data.UserToolEvent;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Path("/data")
@Api(value = "Janelia Workstation Domain Data")
public class UserWebService extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(UserWebService.class);

    @Context
    SecurityContext securityContext;

    @Context
    HttpHeaders headers;
    ActivityLogHelper activityLog = ActivityLogHelper.getInstance();

    LDAPProvider authenticator;

    public UserWebService() {
        register(JacksonFeature.class);
        authenticator = new LDAPProvider();
        authenticator.init();
    }

    @GET
    @Path("/login")
    @ApiOperation(value = "Logs a user in and generates a JSON Web Token",
            notes = "Uses Basic Authentication to check the user against LDAP"
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully attempted login", response=Subject.class),
            @ApiResponse( code = 500, message = "Internal Server Error trying to login" )
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Subject loginSubject() {
        StopWatch stopWatch = new StopWatch();
        // user authentication
        BasicAuthToken userInfo = (BasicAuthToken)getCredentials();
        log.debug("loginSubject({})", userInfo.getUsername());
        if (authenticator.login(userInfo)) {
            // check subjects, if subject doesn't exist for this user and they are in jacsdata,
            // create the account
            DomainDAL dao = DomainDAL.getInstance();
            try {
                Subject user = dao.getSubjectByKey("user:" + userInfo.getUsername());
                if (user==null) {
                    // create a general workstation account for this user since they authenticate against LDAP
                    Subject newUser = authenticator.generateSubjectInfo(userInfo.getUsername());
                    return dao.save(newUser);
                }
                return user;
            }
            catch (Exception e) {
                log.error("Error occurred authenticating user",e);
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            } finally {
                activityLog.logRESTServiceCall("user:" + userInfo.getUsername(), "GET", "/data/login", stopWatch.getTime());
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
    @ApiOperation(value = "Get a List of the Workstation Users",
            notes = ""
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully got list of workstation users", response=Subject.class,
                    responseContainer = "List"),
            @ApiResponse( code = 500, message = "Internal Server Error getting list of workstation users" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<Subject> getSubjects() {
        log.debug("getSubjects()");
        StopWatch stopWatch = new StopWatch();
        DomainDAL dao = DomainDAL.getInstance();
        try {
            return dao.getSubjects();
        }
        catch (Exception e) {
            log.error("Error occurred getting subjects",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            activityLog.logRESTServiceCall(null, "GET", "/data/user/subjects", stopWatch.getTime());
        }
    }


    @GET
    @Path("/user/subject")
    @ApiOperation(value = "Get a List of the Workstation Users",
            notes = ""
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully got list of workstation users", response=Subject.class,
                    responseContainer = "List"),
            @ApiResponse( code = 500, message = "Internal Server Error getting list of workstation users" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Subject getSubjectByKey(@QueryParam("subjectKey") String subjectKey) {
        log.debug("getSubjectByKey({})", subjectKey);
        StopWatch stopWatch = new StopWatch();
        DomainDAL dao = DomainDAL.getInstance();
        try {
            return dao.getSubjectByKey(subjectKey);
        }
        catch (Exception e) {
            log.error("Error occurred finding subject " + subjectKey,e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            activityLog.logRESTServiceCall(subjectKey, "GET", "/data/user/subject", stopWatch.getTime());
        }
    }

    @GET
    @Path("/user/preferences")
    @ApiOperation(value = "Get a List of the User's Preferences",
            notes = ""
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully got user preferences", response=Preference.class,
                    responseContainer = "List"),
            @ApiResponse( code = 500, message = "Internal Server Error getting user preferences" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<Preference> getPreferences(@ApiParam @QueryParam("subjectKey") String subjectKey) {
        log.debug("getPreferences({})",subjectKey);
        StopWatch stopWatch = new StopWatch();
        DomainDAL dao = DomainDAL.getInstance();
        try {
            return dao.getPreferences(subjectKey);
        }
        catch (Exception e) {
            log.error("Error occurred getting preferences",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            activityLog.logRESTServiceCall(subjectKey, "GET", "/data/user/preferences", stopWatch.getTime());
        }
    }

    @PUT
    @Path("/user/preferences")
    @ApiOperation(value = "Sets User Preferences",
            notes = "uses the Preferences Parameter of the DomainQuery."
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully set user preferences", response=Preference.class),
            @ApiResponse( code = 500, message = "Internal Server Error setting user preferences" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Preference setPreferences(DomainQuery query) {
        log.debug("setPreferences({})",query);
        StopWatch stopWatch = new StopWatch();
        DomainDAL dao = DomainDAL.getInstance();
        try {
            return dao.save(query.getSubjectKey(), query.getPreference());
        }
        catch (Exception e) {
            log.error("Error occurred setting preferences",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            activityLog.logRESTServiceCall(query.getSubjectKey(), "PUT", "/data/user/preferences", stopWatch.getTime());
        }
    }

    @PUT
    @Path("/user/permissions")
    @ApiOperation(value = "Changes the permissions for a DomainObject",
            notes = "uses a map (targetClass=domainObject class, granteeKey = user subject key, grant = boolean flag," +
                    "rights = read, write, targetId=Id of the domainObject"
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully got user preferences", response=Preference.class,
                    responseContainer = "List"),
            @ApiResponse( code = 500, message = "Internal Server Error getting user preferences" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void changePermissions(@ApiParam Map<String, Object> params) {
        log.debug("changePermissions({})",params);
        StopWatch stopWatch = new StopWatch();
        DomainDAL dao = DomainDAL.getInstance();
        try {
            dao.changePermissions((String) params.get("subjectKey"), (String) params.get("targetClass"), (Long)params.get("targetId"),
                    (String) params.get("granteeKey"), (String) params.get("rights"), ((Boolean) params.get("grant")).booleanValue());
        }
        catch (Exception e) {
            log.error("Error occurred setting permissions",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            activityLog.logRESTServiceCall(null, "PUT", "/data/user/permissions", stopWatch.getTime());
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