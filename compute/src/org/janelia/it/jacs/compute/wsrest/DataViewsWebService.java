package org.janelia.it.jacs.compute.wsrest;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

@Path("/")
public class DataViewsWebService extends ResourceConfig {
    @Context
    SecurityContext securityContext;

    DomainDAO dao;

    public DomainDAO getDao() {
        if (dao==null) {
            dao = WebServiceContext.getDomainManager();
        }
        return dao;
    }

    public void setDao(DomainDAO dao) {
        this.dao = dao;
        WebServiceContext.setDomainManager(dao);
    }

    public DataViewsWebService() {
        register(JacksonJsonProvider.class);
    }

    @POST
    @Path("/domainobject/details")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getObjectDetails(DomainQuery query) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<DomainObject> detailObjects = null;
            if (query.getReferences()!=null) {
                detailObjects = dao.getDomainObjects(query.getSubjectKey(), query.getReferences());
            } else if (query.getObjectIds()!=null) {
                detailObjects = dao.getDomainObjects(query.getSubjectKey(), query.getObjectType(),
                        query.getObjectIds());
            }
            return mapper.writeValueAsString(detailObjects);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @POST
    @Path("/domainobject")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String updateObjectProperty(DomainQuery query) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            DomainObject updateObj = null;
            // TO DO: add check that parameters are valid
            List<Long> objIds = query.getObjectIds();
            if (objIds!=null && objIds.size()>0) {
                updateObj = dao.updateProperty(query.getSubjectKey(), query.getObjectType(), objIds.get(0),
                        query.getPropertyName(), query.getPropertyValue());
            }
            return mapper.writeValueAsString(updateObj);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @PUT
    @Path("/filter")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String createFilter(@QueryParam("subjectKey") final String subjectKey,
                               Filter filter) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Filter newFilter = dao.save(subjectKey, filter);
            return mapper.writeValueAsString(newFilter);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @POST
    @Path("/filter")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String updateFilter(@QueryParam("subjectKey") final String subjectKey,
                               Filter filter) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Filter newFilter = dao.save(subjectKey, filter);
            return mapper.writeValueAsString(newFilter);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}