package org.janelia.it.jacs.compute.wsrest;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class DataViewsWebService extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(DataViewsWebService.class);

    @Context
    SecurityContext securityContext;

    public DataViewsWebService() {
        register(JacksonFeature.class);
    }

    @POST
    @Path("/domainobject/details")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<DomainObject> getObjectDetails(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            List<DomainObject> detailObjects = null;
            if (query.getReferences()!=null) {
                detailObjects = dao.getDomainObjects(query.getSubjectKey(), query.getReferences());
            } else if (query.getObjectIds()!=null) {
                detailObjects = dao.getDomainObjects(query.getSubjectKey(), query.getObjectType(),
                        query.getObjectIds());
            }
            return detailObjects;
        } catch (Exception e) {
            log.error("Error occurred processing Object Details " + e.getMessage());
        }
        return null;
    }

    @POST
    @Path("/domainobject")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public DomainObject updateObjectProperty(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            DomainObject updateObj = null;
            // TO DO: add check that parameters are valid
            List<Long> objIds = query.getObjectIds();
            if (objIds!=null && objIds.size()>0) {
                updateObj = dao.updateProperty(query.getSubjectKey(), query.getObjectType(), objIds.get(0),
                        query.getPropertyName(), query.getPropertyValue());
            }
            return updateObj;

        } catch (Exception e) {
            log.error("Error occurred processing Domain Object Update Property " + e.getMessage());
        }
        return null;
    }

    @PUT
    @Path("/filter")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Filter createFilter(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            Filter newFilter = (Filter)dao.save(query.getSubjectKey(), query.getDomainObject());
            return newFilter;
        } catch (Exception e) {
            log.error("Error occurred creating Search Filter " + e.getMessage());
        }
        return null;
    }

    @POST
    @Path("/filter")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Filter updateFilter(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            Filter newFilter = (Filter)dao.save(query.getSubjectKey(), query.getDomainObject());
            return newFilter;
        } catch (Exception e) {
            log.error("Error occurred updating search filter " + e.getMessage());
        }
        return null;
    }
}