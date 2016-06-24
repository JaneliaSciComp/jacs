package org.janelia.it.jacs.compute.wsrest.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.bson.Document;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.compute.access.domain.DomainDAL;
import org.janelia.it.jacs.compute.access.mongodb.DomainDAOManager;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/data")
@Api(value = "Janelia Workstation Domain Data")
public class DataSetWebService extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(DataSetWebService.class);

    @Context
    SecurityContext securityContext;

    public DataSetWebService() {
        register(JacksonFeature.class);
    }

    @GET
    @Path("/dataset")
    @ApiOperation(value = "Gets a List of DataSets for the User",
            notes = "Uses the subject key to return a list of DataSets for the user"
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully fetched the list of datasets",  response = DataSet.class,
                    responseContainer = "List" ),
            @ApiResponse( code = 500, message = "Internal Server Error fetching teh datasets" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<DataSet> getDataSets(@ApiParam @QueryParam("subjectKey") final String subjectKey) {
        DomainDAL dao = DomainDAL.getInstance();
        try {
            return dao.getDataSets(subjectKey);
        } catch (Exception e) {
            log.error("Error occurred getting datasets",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/dataset/pipeline")
    @ApiOperation(value = "Gets the default pipelines for datasets",
            notes = "Uses the subject key to return a list of DataSets for the user"
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully fetched list of dataset-pipeline matches",  response = Map.class),
            @ApiResponse( code = 500, message = "Internal Server Error list of dataset-pipeline matches" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String,String> getDatasetPipelines() {
        DomainDAL dao = DomainDAL.getInstance();
        try {
            Collection<DataSet> dataSets = dao.getDataSets(null);
            Map<String,String> results = new HashMap<>();
            for (DataSet dataSet: dataSets) {
                List<String> pipelines = dataSet.getPipelineProcesses();
                if (pipelines!=null) {
                    for (String pipeline: pipelines) {
                        results.put(dataSet.getIdentifier(), "PipelineConfig_" + pipeline);
                    }
                }
            }
            return results;
        } catch (Exception e) {
            log.error("Error occurred getting dataset-pipeline information",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/dataSet/sage")
    @ApiOperation(value = "Gets Sage sync Data Set",
            notes = ""
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully fetched list of datasets synced with SAGE",   response = DataSet.class,
                    responseContainer = "List"),
            @ApiResponse( code = 500, message = "Internal Server Error list of dataset synced with SAGE" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<DataSet> getSageSyncDataSets(@ApiParam @QueryParam("owners") final List<String> owners,
                                             @ApiParam @QueryParam("sageSync") final Boolean sageSync) {
        DomainDAL dao = DomainDAL.getInstance();
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
    @ApiOperation(value = "Creates a DataSet using the DomainObject parameter of the DomainQuery",
            notes = ""
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully created a DataSet",
                    response = DataSet.class),
            @ApiResponse( code = 500, message = "Internal Server Error creating a dataset" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public DataSet createDataSet(DomainQuery query) {
        DomainDAL dao = DomainDAL.getInstance();
        try {
            return dao.save(query.getSubjectKey(), (DataSet)query.getDomainObject());
        } catch (Exception e) {
            log.error("Error occurred creating DataSet ",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @Path("/dataset")
    @ApiOperation(value = "Updates a DataSet using the DomainObject parameter of the DomainQuery",
            notes = ""
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully updated a DataSet",
                    response = DataSet.class),
            @ApiResponse( code = 500, message = "Internal Server Error updating a dataset" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public DataSet updateDataSet(@ApiParam DomainQuery query) {
        DomainDAL dao = DomainDAL.getInstance();
        try {
            return dao.save(query.getSubjectKey(), (DataSet)query.getDomainObject());
        } catch (Exception e) {
            log.error("Error occurred updating data set ",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }


    @DELETE
    @Path("/dataset")
    @ApiOperation(value = "Removes the DataSet using the DataSet Id",
            notes = ""
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully removed a DataSet"),
            @ApiResponse( code = 500, message = "Internal Server Error removing a dataset" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    public void removeDataSet(@ApiParam @QueryParam("subjectKey") final String subjectKey,
                              @ApiParam @QueryParam("dataSetId") final String dataSetId) {
        DomainDAL dao = DomainDAL.getInstance();
        Reference dataSetRef = Reference.createFor(DataSet.class, new Long(dataSetId));
        try {
            DomainObject domainObj = dao.getDomainObject(subjectKey, dataSetRef);
            dao.deleteDomainObject(subjectKey, domainObj);
        } catch (Exception e) {
            log.error("Error occurred removing dataset",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/dataset/all")
    @ApiOperation(value = "Gets a distinct list of all datasets",
            notes = "")
    public String getAllDatasets() {
        DomainDAO dao = DomainDAOManager.getInstance().getDao();
        MongoClient m = dao.getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        MongoCollection<Document> dataSet = db.getCollection("dataSet");
        try {
            List<String> jsonResult = dataSet.distinct("name",String.class)
                    .into(new ArrayList());
            jsonResult.remove(0);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            return objectMapper.writeValueAsString(jsonResult);
        } catch (Exception e) {
            log.error("Error occurred getting list of datasets",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }


}