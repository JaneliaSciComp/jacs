package org.janelia.it.jacs.compute.wsrest.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;


import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.compute.launcher.indexing.IndexingHelper;
import org.janelia.it.jacs.compute.wsrest.WebServiceContext;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/data")
public class DataSetWebService extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(DataSetWebService.class);

    @Context
    SecurityContext securityContext;

    public DataSetWebService() {
        register(JacksonFeature.class);
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
            log.error("Error occurred getting datasets",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/dataset/sage")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<DataSet> getSageSyncDataSets(@QueryParam("owners") final List<String> owners,
                                             @QueryParam("sageSync") final Boolean sageSync) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            List<DataSet> listDataSets = new ArrayList<>();
            if (owners==null) {
                listDataSets = dao.getDataSets(null);
            } else {
                for (String owner: owners) {
                    List<DataSet> ownerDataSets = dao.getDataSets(owner);
                    if (ownerDataSets!=null) {
                        if (sageSync) {
                            for (DataSet dataSet: ownerDataSets) {
                                if (dataSet.isSageSync()) {
                                    listDataSets.add(dataSet);
                                }
                            }
                        } else {
                            listDataSets.addAll(ownerDataSets);
                        }
                    }
                }
            }
            return listDataSets;
        } catch (Exception e) {
            log.error("Error occurred getting datasets using sageSync filter",e);
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
            IndexingHelper.sendReindexingMessage(newDataSet);
            return newDataSet;
        } catch (Exception e) {
            log.error("Error occurred creating DataSet ",e);
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
            DataSet updateDataSet = (DataSet)dao.save(query.getSubjectKey(), query.getDomainObject());
            IndexingHelper.sendReindexingMessage(updateDataSet);
            return updateDataSet;
        } catch (Exception e) {
            log.error("Error occurred updating data set ",e);
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
            DomainObject domainObj = dao.getDomainObject(subjectKey, dataSetRef);
            IndexingHelper.sendRemoveFromIndexMessage(domainObj.getId());
            dao.remove(subjectKey, domainObj);
        } catch (Exception e) {
            log.error("Error occurred removing dataset",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }


}