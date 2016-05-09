package org.janelia.it.jacs.compute.wsrest.info;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
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
import io.swagger.annotations.ApiResponses;
import org.bson.Document;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.compute.wsrest.WebServiceContext;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.*;
import static java.util.Arrays.asList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/info")
@Api(value = "Janelia Workstation Informatics")
public class StatusWebService extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(StatusWebService.class);

    @Context
    SecurityContext securityContext;

    public StatusWebService() {
        register(JacksonFeature.class);
    }

    @GET
    @Path("/sample")
    @ApiOperation(value = "Gets Status information for a sample",
            notes = "")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getLSMImageInfo(@QueryParam("totals") final Boolean totals,
                                  @QueryParam("status") final String status) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        MongoClient m = dao.getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        MongoCollection<Document> sample = db.getCollection("sample");
        List<Document> jsonResult = new ArrayList<>();
        try {
            if (totals!=null && totals.booleanValue()) {
                // get image counts by status
                log.info(Long.toString(System.currentTimeMillis()));
                jsonResult = sample.aggregate(asList(
                        new Document("$group", new Document("_id", "$status").append("count", new Document("$sum", 1))))).into(new ArrayList());
                log.info(Long.toString(System.currentTimeMillis()));


            } else {
                // get a list of sample info (name, ownerKey, updatedDate) by Status
                log.info(Long.toString(System.currentTimeMillis()));
                if (status!=null) {
                    jsonResult = sample.find(eq("status", status)).batchSize(1000000).projection(fields(include("name", "ownerKey", "updatedDate"),
                            excludeId())).into(new ArrayList());
                } else {
                    jsonResult = sample.find().batchSize(1000000).projection(fields(include("name", "ownerKey", "updatedDate", "status"),
                            excludeId())).into(new ArrayList());
                }
                log.info(Long.toString(System.currentTimeMillis()));
            }
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            String moo =  objectMapper.writeValueAsString(jsonResult);
            log.info(Long.toString(System.currentTimeMillis()));
            return moo;
        } catch (Exception e) {
            log.error("Error occurred getting datasets",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/sample/blockview")
    @ApiOperation(value = "Gets blockview information",
            notes = "")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getBlockView(@QueryParam("startDate") final String startDate,
                               @QueryParam("endDate") final String endDate) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        MongoClient m = dao.getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        MongoCollection<Document> sample = db.getCollection("sample");
        List<Document> jsonResult = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            if (startDate!=null || endDate!=null) {
                if (startDate==null) {
                    Date endDateTime = sdf.parse(endDate);
                    jsonResult = sample.find(lte("tmogDate", endDateTime))
                            .batchSize(1000000)
                            .projection(fields(include("tmogDate", "status", "dataSet", "name")))
                            .sort(orderBy(ascending("tmogDate"),ascending("_id")))
                            .into(new ArrayList());
                } else if (endDate==null) {
                    Date startDateTime = sdf.parse(startDate);
                    jsonResult = sample.find(gte("tmogDate", startDateTime))
                            .batchSize(1000000)
                            .projection(fields(include("tmogDate", "status", "dataSet", "name")))
                            .sort(orderBy(ascending("tmogDate"), ascending("_id")))
                            .into(new ArrayList());
                } else {
                    Date endDateTime = sdf.parse(endDate);
                    Date startDateTime = sdf.parse(startDate);
                    jsonResult = sample.find(and(
                            gte("tmogDate", startDateTime),
                            lte("tmogDate", endDateTime)))
                            .batchSize(1000000)
                            .projection(fields(include("tmogDate", "status", "dataSet", "name")))
                            .sort(orderBy(ascending("tmogDate"), ascending("_id")))
                            .into(new ArrayList());
                }
            } else {
                jsonResult = sample.find().batchSize(1000000)
                        .projection(fields(include("tmogDate", "status", "dataSet", "name")))
                        .sort(orderBy(ascending("tmogDate"), ascending("_id")))
                        .into(new ArrayList());
            }
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            String samples =  objectMapper.writeValueAsString(jsonResult);
            return samples;
        } catch (Exception e) {
            log.error("Error occurred getting samples",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/sample/imagecompletion")
    @ApiOperation(value = "Gets Sample Inage Completion statistics",
            notes = "")

    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getImageCompletion(@QueryParam("line") final String line,
                                     @QueryParam("slideCode") final String slideCode) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        MongoClient m = dao.getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        MongoCollection<Document> sample = db.getCollection("sample");
        List<Document> jsonResult = new ArrayList<>();
        try {
            jsonResult = sample.find(
                    and(not(regex("status", ".*-Retired")),
                            not(regex("status", ".*~.*")),
                            eq("slideCode",slideCode),
                            eq("line",line)))
                    .batchSize(1000000)
                    .projection(fields(include("tmogDate", "status", "dataSet", "name")))
                    .sort(orderBy(descending("_id")))
                    .into(new ArrayList());
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            String samples =  objectMapper.writeValueAsString(jsonResult);
            return samples;
        } catch (Exception e) {
            log.error("Error occurred getting image completion",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/image/cycletime")
    @ApiOperation(value = "Gets Cycletime information for an image",
            notes = "")

    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getImageCycleTime() {
        DomainDAO dao = WebServiceContext.getDomainManager();
        MongoClient m = dao.getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        MongoCollection<Document> image = db.getCollection("image");
        List<Document> jsonResult = new ArrayList<>();
        try {
            jsonResult = image.aggregate(asList(
                    new Document("$project", new Document("line", "$line")
                            .append("slideCode", "$slideCode")
                            .append("dataSet", "$dataSet")
                            .append("tmogDate", "$tmogDate")
                            .append("creationDate", "$creationDate")
                            .append("completionDate", "$completionDate")
                            .append("cycleTime",
                                    new Document("$subtract", asList("$creationDate", "$tmogDate"))))))
                    .into(new ArrayList());
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            String images =  objectMapper.writeValueAsString(jsonResult);
            return images;
        } catch (Exception e) {
            log.error("Error occurred getting image cycle times",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/sample/pipelinestatus")
    @ApiOperation(value = "Gets pipeline status for a sample",
            notes = "")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getSamplePipelineStatus() {
        DomainDAO dao = WebServiceContext.getDomainManager();
        MongoClient m = dao.getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        MongoCollection<Document> sample = db.getCollection("sample");
        List<Document> jsonResult = new ArrayList<>();
        try {
            jsonResult = sample.aggregate(asList(
                    new Document("$project", new Document("name", "$name")
                            .append("dataSet", "$dataSet")
                            .append("creationDate", "$creationDate")
                            .append("completionDate", "$completionDate")
                            .append("pipelineTime",
                                    new Document("$subtract", asList(new Date(), "$creationDate"))))))
                    .into(new ArrayList());
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            String images =  objectMapper.writeValueAsString(jsonResult);
            return images;
        } catch (Exception e) {
            log.error("Error occurred getting image cycle times",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }


    @GET
    @Path("/sample/errorbydataset")
    @ApiOperation(value = "Gets all the errors for a DataSet",
            notes = "")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getSampleErrorsByDataSet() {
        DomainDAO dao = WebServiceContext.getDomainManager();
        MongoClient m = dao.getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        MongoCollection<Document> sample = db.getCollection("sample");
        List<Document> jsonResult = new ArrayList<>();
        try {
            jsonResult = sample.aggregate(asList(
                    new Document("$match", new Document("status", "Error")),
                    new Document("$group", new Document("_id", "$dataSet")
                            .append("count", new Document("$sum", "1")))))
                    .into(new ArrayList());
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            String images =  objectMapper.writeValueAsString(jsonResult);
            return images;
        } catch (Exception e) {
            log.error("Error occurred getting sample error counts by dataset",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }



    @GET
    @Path("/dataset/sageimagery")
    @ApiOperation(value = "Gets All the DataSets for an Owner",
            notes = "")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getDatasetsByOwner() {
        DomainDAO dao = WebServiceContext.getDomainManager();
        MongoClient m = dao.getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        MongoCollection<Document> dataSet = db.getCollection("dataSet");
        List<Document> jsonResult = new ArrayList<>();
        try {
            jsonResult = dataSet.find()
                    .batchSize(1000000)
                    .projection(fields(include("identifier", "name", "ownerKey")))
                    .sort(orderBy(ascending("identifier")))
                    .into(new ArrayList());
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            String images =  objectMapper.writeValueAsString(jsonResult);
            return images;
        } catch (Exception e) {
            log.error("Error occurred getting sample error counts by dataset",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }


    @GET
    @Path("/sample/search")
    @ApiOperation(value = "Searchs Samples based off search parameters",
            notes = "")
    @ApiResponses(value = {

    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getSampleName() {
        DomainDAO dao = WebServiceContext.getDomainManager();
        MongoClient m = dao.getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        MongoCollection<Document> dataSet = db.getCollection("dataSet");
        List<Document> jsonResult = new ArrayList<>();
        try {
            jsonResult = dataSet.find(not(regex("name", ".*~.*")))
                    .batchSize(1000000)
                    .projection(fields(include("name")))
                    .sort(orderBy(ascending("name")))
                    .into(new ArrayList());
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            String images =  objectMapper.writeValueAsString(jsonResult);
            return images;
        } catch (Exception e) {
            log.error("Error occurred getting sample error counts by dataset",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }


    @GET
    @Path("/sample/workstationstatus")
    @ApiOperation(value = "Gets the Status for a Sample given start and end dates for tmog and completion",
            notes = "")
    @ApiResponses(value = {

    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getSampleStatusByDate(@QueryParam("startDate") final String startDate,
                                        @QueryParam("endDate") final String endDate) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        MongoClient m = dao.getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        MongoCollection<Document> sample = db.getCollection("sample");
        List<Document> jsonResult = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
        try {
            Date startDateTime = sdf.parse(startDate);
            if (endDate!=null) {
                Date endDateTime = sdf.parse(endDate);
                jsonResult = sample.find(and(
                        gte("tmogDate", startDateTime),
                        lte("tmogDate", endDateTime)))
                        .batchSize(1000000)
                        .projection(fields(include("name", "line", "slideCode", "status")))
                        .into(new ArrayList());
            } else {
                jsonResult = sample.find(gte("tmogDate", startDateTime))
                        .batchSize(1000000)
                        .projection(fields(include("name", "line", "slideCode", "status")))
                        .into(new ArrayList());
            }
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            String images =  objectMapper.writeValueAsString(jsonResult);
            return images;
        } catch (Exception e) {
            log.error("Error occurred getting sample error counts by dataset",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}