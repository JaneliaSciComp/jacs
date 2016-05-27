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
import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.sample.Image;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleTile;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.*;
import static java.util.Arrays.asList;

import org.jongo.MongoCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/info")
@Api(value = "Janelia Workstation Informatics")
public class SampleStatusWebService extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(SampleStatusWebService.class);

    @Context
    SecurityContext securityContext;

    public SampleStatusWebService() {
        register(JacksonFeature.class);
    }

    @GET
    @Path("/sample")
    @ApiOperation(value = "Gets Status information for a sample",
            notes = "")
    public String getLSMImageInfo(@QueryParam("totals") final Boolean totals,
                                  @QueryParam("status") final String status) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        MongoClient m = dao.getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        MongoCollection<Document> sample = db.getCollection("sample");
        List<Document> jsonResult = new ArrayList<>();
        try {
            if (totals!=null && totals.booleanValue()) {
                jsonResult = sample.aggregate(asList(
                        new Document("$group", new Document("_id", "$status").append("count", new Document("$sum", 1))))).into(new ArrayList());
            } else {
                // get a list of sample info (name, ownerKey, updatedDate) by Status
                if (status!=null) {
                    jsonResult = sample.find(eq("status", status)).batchSize(1000000).projection(fields(include("name", "ownerKey", "updatedDate"),
                            excludeId())).into(new ArrayList());
                } else {
                    jsonResult = sample.find().batchSize(1000000).projection(fields(include("name", "ownerKey", "updatedDate", "status"),
                            excludeId())).into(new ArrayList());
                }
            }
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            return objectMapper.writeValueAsString(jsonResult);
        } catch (Exception e) {
            log.error("Error occurred getting datasets",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/sample/blockview")
    @ApiOperation(value = "Gets blockview information",
            notes = "")
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
            log.error("Error occurred getting samples", e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/sample/attribute")
    @ApiOperation(value = "Gets Sample Completion statistics",
            notes = "")
    public String getSampleInformation(@QueryParam("sampleId") final Long sampleId,
                                     @QueryParam("attribute") final String attribute) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        MongoClient m = dao.getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        MongoCollection<Document> sample = db.getCollection("sample");
        try {
            Document jsonResult = sample.find(eq("_id", sampleId))
                    .batchSize(1000000)
                    .projection(fields(include("name", "tmogDate", "status", "dataSet", "slideCode", "line", attribute)))
                     .first();
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            return objectMapper.writeValueAsString(jsonResult);
        } catch (Exception e) {
            log.error("Error occurred getting image completion",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/sample/custom")
    @ApiOperation(value = "Gets Sample data projection ordered and sorted by attribute",
            notes = "")
    public String getSampleInformation(@QueryParam("attribute") final String attribute) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        MongoClient m = dao.getMongo();
        org.jongo.MongoCollection sample = dao.getCollectionByName("sample");
        try {
            if (attribute==null) {
                return "attribute is a required parameter for this method";
            }
            List<String> results = sample.distinct(attribute).as(String.class);
            java.util.Collections.sort(results);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            return objectMapper.writeValueAsString(results);
        } catch (Exception e) {
            log.error("Error occurred getting image completion",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/sample/imagecompletion")
    @ApiOperation(value = "Gets Image Completion statistics",
            notes = "")
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

    private MongoCollection<Document> getNativeCollection(String collectionName) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        MongoClient m = dao.getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        return db.getCollection(collectionName);
    }

    @GET
    @Path("/sample/pipelinestatus")
    @ApiOperation(value = "Gets pipeline status for a sample",
            notes = "")
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
    @Path("/sample/statuscounts")
    @ApiOperation(value = "Bins all the samples by status",
            notes = "")
    public String getSampleStatus() {
        DomainDAO dao = WebServiceContext.getDomainManager();
        MongoClient m = dao.getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        MongoCollection<Document> sample = db.getCollection("sample");
        List<Document> jsonResult = new ArrayList<>();
        try {
            jsonResult = sample.aggregate(asList(
                    new Document("$group", new Document("_id", "$status")
                            .append("count", new Document("$sum", 1)))))
                    .into(new ArrayList());
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            String images =  objectMapper.writeValueAsString(jsonResult);
            return images;
        } catch (Exception e) {
            log.error("Error occurred getting sample status counts by dataset",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/sample/errormips")
    @ApiOperation(value = "Returns samples by error and their mips",
            notes = "")
    public String getSampleErrorMips(@QueryParam("dataset") final String dataset) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        MongoClient m = dao.getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        MongoCollection<Document> sample = db.getCollection("sample");
        List<Document> jsonResult = new ArrayList<>();
        List<Document> aggregateCommands = new ArrayList<Document>();
        if (dataset!=null) {
            aggregateCommands.add(new Document("$match", new Document("dataSet",dataset)));
        }
        try {
            aggregateCommands.add(new Document("$match", new Document("objectiveSamples.pipelineRuns.error",
                    new Document("$exists", true))));
            aggregateCommands.add(new Document("$match", new Document("objectiveSamples.tiles.files.SignalMip",
                            new Document("$exists", true))));
            aggregateCommands.add(new Document("$project", new Document("_id",0)
                            .append("sampleName", "$name")
                            .append("dataSet", "$dataSet")
                            .append("image", "$objectiveSamples.tiles.files.SignalMip")));
            aggregateCommands.add(new Document("$sort", new Document ("sampleName", 1)));
            jsonResult = sample.aggregate(aggregateCommands).into(new ArrayList());
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            return objectMapper.writeValueAsString(jsonResult);
        } catch (Exception e) {
            log.error("Error occurred getting sample status counts by dataset",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/sample/errors")
    @ApiOperation(value = "returns information about pipeline errors by sample",
            notes = "")
    public String getSampleErrors() {
        DomainDAO dao = WebServiceContext.getDomainManager();
        MongoClient m = dao.getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        MongoCollection<Document> sample = db.getCollection("sample");
        List<Document> jsonResult = new ArrayList<>();
        try {
            jsonResult = sample.aggregate(asList(
                    new Document("$match", new Document("objectiveSamples.pipelineRuns.error",
                            new Document ("$exists", true))),
                    new Document("$unwind", "$objectiveSamples"),
                    new Document("$unwind", "$objectiveSamples.pipelineRuns"),
                    new Document("$match", new Document("objectiveSamples.pipelineRuns.error",
                            new Document ("$exists", true))),
                    new Document("$project", new Document("sampleName","$name")
                            .append("_id",0)
                            .append("dataSet","$dataSet")
                            .append("classification", "$objectiveSamples.pipelineRuns.error.classification")
                            .append("description", "$objectiveSamples.pipelineRuns.error.description")),
                    new Document("$sort", new Document ("sampleName", 1))))
                    .into(new ArrayList());
            for (int i=0; i<jsonResult.size(); i++) {
                if (!jsonResult.get(i).containsKey("classification")) {
                    jsonResult.get(i).put("classification", "UnclassifiedError");
                }
            }
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(jsonResult);
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
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getSampleBySearchParameters(@QueryParam("name") final String name,
                                              @QueryParam("slideCode") final String slideCode,
                                              @QueryParam("line") final String line,
                                              @QueryParam("dataSet") final String dataSet) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        org.jongo.MongoCollection sample = dao.getCollectionByName("sample");

        List<String> formattedResults = new ArrayList<String>();
        try {
            MongoCursor<Sample> results;
            String query = null;
            String field = null;
            if (name!=null) {
                field = "name";
                query = "{name:'" + name + "'}";
            } else if (slideCode!=null) {
                field = "slideCode";
                query = "{slideCode:'" + slideCode + "'}";
            } else if (line!=null) {
                field = "line";
                query = "{line:'" + line + "'}";
            } else if (dataSet!=null) {
                field = "dataSet";
                query = "{dataSet:'" + dataSet + "'}";
            }

            results = sample.find(query).as(Sample.class);
            while (results.hasNext()) {
                Sample result = results.next();
                for (ObjectiveSample objective : result.getObjectiveSamples()) {
                    String defaultFilepath = objective.getLatestRun().getLatestResult().getFiles().get(FileType.SignalMip);
                    if (defaultFilepath==null) {
                        defaultFilepath = objective.getLatestRun().getLatestResult().getFiles().get(FileType.AllMip);
                    }
                    if (defaultFilepath!=null) {
                        formattedResults.add("{name:'" + result.getName() + "'," +
                                        "line:'" + result.getLine() + "'," +
                                        "slideCode:'" + result.getSlideCode()  + "'," +
                                        "effector:'" + result.getEffector()  + "'," +
                                        "dataSet:'" + result.getDataSet()  + "'," +
                                        "image:'" + defaultFilepath + "'}"
                        );
                    } else {
                        formattedResults.add("{name:'" + result.getName() + "'," +
                                "line:'" + result.getLine() + "'," +
                                "slideCode:'" + result.getSlideCode()  + "'," +
                                "effector:'" + result.getEffector()  + "'," +
                                "dataSet:'" + result.getDataSet()  + "'}");
                    }
                }
            }
            return formattedResults;
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
    public String getSampleStatusByDate(@QueryParam("startDate") final String startDate,
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
                            .projection(fields(include("name", "line", "slideCode", "status")))
                            .into(new ArrayList());
                } else if (endDate==null) {
                    Date startDateTime = sdf.parse(startDate);
                    jsonResult = sample.find(gte("tmogDate", startDateTime))
                            .batchSize(1000000)
                            .projection(fields(include("name", "line", "slideCode", "status")))
                            .into(new ArrayList());
                } else {
                    Date endDateTime = sdf.parse(endDate);
                    Date startDateTime = sdf.parse(startDate);
                    jsonResult = sample.find(and(
                            gte("tmogDate", startDateTime),
                            lte("tmogDate", endDateTime)))
                            .batchSize(1000000)
                            .projection(fields(include("name", "line", "slideCode", "status")))
                            .into(new ArrayList());
                }
            } else {
                jsonResult = sample.find().batchSize(1000000)
                        .projection(fields(include("name", "line", "slideCode", "status")))
                        .into(new ArrayList());
            }

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            String images =  objectMapper.writeValueAsString(jsonResult);
            return images;
        } catch (Exception e) {
            log.error("Error occurred getting sample error counts by dataset",e);
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }
}