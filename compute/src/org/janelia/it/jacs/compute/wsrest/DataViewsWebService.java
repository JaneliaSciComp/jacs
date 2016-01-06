package org.janelia.it.jacs.compute.wsrest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.sample.DataSet;
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
            log.error("Error occurred processing Object Details " + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
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
            log.error("Error occurred processing Domain Object Update Property " + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/dataset")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<DataSet> getDataSets(@QueryParam("subjectKey") final String subjectKey) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            Collection<DataSet> dataSets = dao.getDataSets(subjectKey);
            return new ArrayList<DataSet>(dataSets);
        } catch (Exception e) {
            log.error("Error occurred getting datasets" + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @PUT
    @Path("/dataset")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public DataSet createDataSet(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            DataSet newDataSet = (DataSet)dao.save(query.getSubjectKey(), query.getDomainObject());
            return newDataSet;
        } catch (Exception e) {
            log.error("Error occurred creating DataSet " + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @Path("/dataset")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public DataSet updateDataSet(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            DataSet newDataSet = (DataSet)dao.save(query.getSubjectKey(), query.getDomainObject());
            return newDataSet;
        } catch (Exception e) {
            log.error("Error occurred updating data set " + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }


    @DELETE
    @Path("/dataset")
    @Consumes(MediaType.APPLICATION_JSON)
    public void removeDataSet(@QueryParam("subjectKey") final String subjectKey,
                              @QueryParam("dataSetId") final String dataSetId) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        Reference dataSetRef = new Reference (Annotation.class.getName(), new Long(dataSetId));
        try {
            dao.remove(subjectKey, dao.getDomainObject(subjectKey, dataSetRef));
        } catch (Exception e) {
            log.error("Error occurred removing dataset" + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
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
            log.error("Error occurred creating Search Filter " + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
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
            log.error("Error occurred updating search filter " + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}