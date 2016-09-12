package org.janelia.it.jacs.compute.wsrest.info;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.apache.commons.lang.time.StopWatch;
import org.bson.Document;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.compute.access.mongodb.DomainDAOManager;
import org.janelia.it.jacs.compute.util.ActivityLogHelper;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.sample.SampleProcessingResult;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.*;
import static java.util.Arrays.asList;

import org.janelia.it.jacs.model.domain.support.ResultDescriptor;
import org.janelia.it.jacs.model.domain.support.SampleUtils;
import org.janelia.it.jacs.shared.utils.DateUtil;
import org.jongo.MongoCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/info")
@Api(value = "Janelia Workstation Informatics")
public class SampleStatusWebService extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(SampleStatusWebService.class);

    @Context
    SecurityContext securityContext;
    ActivityLogHelper activityLog = ActivityLogHelper.getInstance();

    public SampleStatusWebService() {
        register(JacksonFeature.class);
    }

    @GET
    @Path("/sample")
    @ApiOperation(value = "Gets Status information for a sample",
            notes = "")
    public String getLSMImageInfo(@QueryParam("totals") final Boolean totals,
                                  @QueryParam("status") final String status) {
        StopWatch stopWatch = new StopWatch();
        DomainDAO dao = DomainDAOManager.getInstance().getDao();
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
        } finally {
            activityLog.logRESTServiceCall(null, "GET", "/info/sample", stopWatch.getTime());
        }
    }

    @GET
    @Path("/sample/blockview")
    @ApiOperation(value = "Gets blockview information",
            notes = "")
    public String getBlockView(@QueryParam("startDate") final String startDate,
                               @QueryParam("endDate") final String endDate) {
        StopWatch stopWatch = new StopWatch();
        DomainDAO dao = DomainDAOManager.getInstance().getDao();
        MongoClient m = dao.getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        MongoCollection<Document> sample = db.getCollection("sample");
        List<Document> jsonResult = new ArrayList<>();
        try {
            if (startDate!=null || endDate!=null) {
                if (startDate==null) {
                    Date endDateTime = DateUtil.createEndDate(endDate);
                    jsonResult = sample.find(lte("tmogDate", endDateTime))
                            .batchSize(1000000)
                            .projection(fields(include("tmogDate", "status", "dataSet", "name")))
                            .sort(orderBy(ascending("tmogDate"),ascending("_id")))
                            .into(new ArrayList());
                } else if (endDate==null) {
                    Date startDateTime = DateUtil.createStartDate(startDate);
                    jsonResult = sample.find(gte("tmogDate", startDateTime))
                            .batchSize(1000000)
                            .projection(fields(include("tmogDate", "status", "dataSet", "name")))
                            .sort(orderBy(ascending("tmogDate"), ascending("_id")))
                            .into(new ArrayList());
                } else {
                    Date endDateTime = DateUtil.createEndDate(endDate);
                    Date startDateTime = DateUtil.createStartDate(startDate);
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

            for (Document result : jsonResult) {
                result.put("tmogDate", DateUtil.formatDate(result.getDate("tmogDate")));
            }
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            String samples =  objectMapper.writeValueAsString(jsonResult);
            return samples;
        } catch (Exception e) {
            log.error("Error occurred getting samples", e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            activityLog.logRESTServiceCall(null, "GET", "/info/sample/blockview", stopWatch.getTime());
        }
    }

    @GET
    @Path("/sample/attribute")
    @ApiOperation(value = "Gets Sample Completion statistics",
            notes = "")
    public String getSampleInformation(@QueryParam("sampleId") final Long sampleId,
                                     @QueryParam("attribute") final String attribute) {
        StopWatch stopWatch = new StopWatch();
        DomainDAO dao = DomainDAOManager.getInstance().getDao();
        MongoClient m = dao.getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        MongoCollection<Document> sample = db.getCollection("sample");
        try {
            Document jsonResult;
            if (attribute==null) {
                jsonResult = sample.find(eq("_id", sampleId))
                        .batchSize(1000000)
                        .first();
            } else {
                jsonResult = sample.find(eq("_id", sampleId))
                        .batchSize(1000000)
                        .projection(fields(include("name", "tmogDate", "status", "dataSet", "slideCode", "line", attribute)))
                        .first();
            }
            jsonResult.put("tmogDate", DateUtil.formatDate(jsonResult.getDate("tmogDate")));

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            return objectMapper.writeValueAsString(jsonResult);
        } catch (Exception e) {
            log.error("Error occurred getting image completion",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            activityLog.logRESTServiceCall(null, "GET", "/info/sample/attribute", stopWatch.getTime());
        }
    }

    @GET
    @Path("/sample/custom")
    @ApiOperation(value = "Gets Sample data projection ordered and sorted by attribute",
            notes = "")
    public String getSampleInformation(@QueryParam("attribute") final String attribute) {
        StopWatch stopWatch = new StopWatch();
        DomainDAO dao = DomainDAOManager.getInstance().getDao();
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
        } finally {
            activityLog.logRESTServiceCall(null, "GET", "/info/sample/custom", stopWatch.getTime());
        }
    }

    @GET
    @Path("/sample/imagecompletion")
    @ApiOperation(value = "Gets Image Completion statistics",
            notes = "")
    public String getImageCompletion(@QueryParam("line") final String line,
                                     @QueryParam("slideCode") final String slideCode) {
        StopWatch stopWatch = new StopWatch();
        DomainDAO dao = DomainDAOManager.getInstance().getDao();
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
            for (Document result : jsonResult) {
                result.put("tmogDate", DateUtil.formatDate(result.getDate("tmogDate")));
            }
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            String samples =  objectMapper.writeValueAsString(jsonResult);
            return samples;
        } catch (Exception e) {
            log.error("Error occurred getting image completion",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            activityLog.logRESTServiceCall(null, "GET", "/info/sample/imagecompletion", stopWatch.getTime());
        }
    }

    private MongoCollection<Document> getNativeCollection(String collectionName) {
        DomainDAO dao = DomainDAOManager.getInstance().getDao();
        MongoClient m = dao.getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        return db.getCollection(collectionName);
    }

    @GET
    @Path("/sample/pipelinestatus")
    @ApiOperation(value = "Gets pipeline status for a sample",
            notes = "")
    public String getSamplePipelineStatus(@QueryParam("hours") final String hours) {
        StopWatch stopWatch = new StopWatch();
        DomainDAO dao = DomainDAOManager.getInstance().getDao();
        MongoClient m = dao.getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        MongoCollection<Document> sample = db.getCollection("sample");
        List<Document> jsonResult = new ArrayList<>();
        int hoursNum = -336;// 14 days by default;
        if (hours!=null) {
            hoursNum = -Integer.parseInt(hours);
        }
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, hoursNum);
        try {
            jsonResult = sample.aggregate(asList(
                    new Document("$match", new Document("creationDate",
                            new Document ("$gte", c.getTime()))),
                    new Document("$project", new Document("name", "$name")
                            .append("dataSet", "$dataSet")
                            .append("creationDate", "$creationDate")
                            .append("completionDate", "$completionDate")
                            .append("status", "$status")
                            .append("pipelineTime",
                                    new Document("$divide", asList(
                                            new Document("$subtract", asList(new Date(), "$creationDate")),
                                            3600000))))))
                    .into(new ArrayList());
            for (Document result : jsonResult) {
                if (result.getString("status")==null)  {
                    result.put("status", "");
                }
                result.put("creationDate", DateUtil.formatDate(result.getDate("creationDate")));
                result.put("completionDate", DateUtil.formatDate(result.getDate("completionDate")));
            }
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            String images =  objectMapper.writeValueAsString(jsonResult);
            return images;
        } catch (Exception e) {
            log.error("Error occurred getting image cycle times",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            activityLog.logRESTServiceCall(null, "GET", "/info/sample/pipelinestatus", stopWatch.getTime());
        }
    }

    @GET
    @Path("/sample/statuscounts")
    @ApiOperation(value = "Bins all the samples by status",
            notes = "")
    public String getSampleStatus() {
        StopWatch stopWatch = new StopWatch();
        DomainDAO dao = DomainDAOManager.getInstance().getDao();
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
        } finally {
            activityLog.logRESTServiceCall(null, "GET", "/info/sample/statuscounts", stopWatch.getTime());
        }
    }

    @GET
    @Path("/sample/errormips")
    @ApiOperation(value = "Returns samples by error and their mips",
            notes = "")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getSampleErrorMips(@QueryParam("dataset") final String dataset) {
        StopWatch stopWatch = new StopWatch();
        DomainDAO dao = DomainDAOManager.getInstance().getDao();
        org.jongo.MongoCollection sample = dao.getCollectionByName("sample");
        StringWriter query = new StringWriter();
        query.append("{");

        List<String> formattedResults = new ArrayList<String>();
        if (dataset!=null) {
            query.append("'dataSet':'" + dataset + "',");
        }
        try {
            query.append("'objectiveSamples.pipelineRuns.error': {$exists: true}}");
            MongoCursor<Sample> results = sample.find(query.toString()).as(Sample.class);
            while (results.hasNext()) {
                Sample result = results.next();
                for (ObjectiveSample objective : result.getObjectiveSamples()) {
                    if (objective.getLatestRun().getError() != null) {
                        List<SamplePipelineRun> sampleProcResult = objective.getPipelineRuns();
                        if (sampleProcResult!=null) {
                            if (sampleProcResult.get(sampleProcResult.size()-1).getError()!=null) {
                                SamplePipelineRun foo = sampleProcResult.get(sampleProcResult.size()-1);
                                List<SampleProcessingResult> moo = foo.getSampleProcessingResults();
                                Map<FileType, String> files = moo.get(moo.size() - 1).getFiles();
                                Map<String, String> labelMap = new HashMap<String, String>();
                                Iterator<FileType> poo = files.keySet().iterator();
                                while (poo.hasNext()) {
                                    FileType goo = poo.next();
                                    labelMap.put(goo.getLabel(), files.get(goo));
                                }
                                if (files != null) {
                                    ObjectMapper objectMapper = new ObjectMapper();
                                    String fileResults = objectMapper.writeValueAsString(labelMap);
                                    formattedResults.add("{sampleName:'" + result.getName() + "'," +
                                                    "dataSet:'" + result.getDataSet() + "'," +
                                                    "image:" + fileResults + "}"
                                    );
                                } else {
                                    formattedResults.add("{sampleName:'" + result.getName() + "'," +
                                            "dataSet:'" + result.getDataSet() + "'}");
                                }
                            }

                        }
                    }


                }
            }
            return formattedResults;
        } catch (Exception e) {
            log.error("Error occurred getting sample status counts by dataset",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            activityLog.logRESTServiceCall(null, "GET", "/info/sample/errormips", stopWatch.getTime());
        }
    }

    @GET
    @Path("/sample/errors")
    @ApiOperation(value = "returns information about pipeline errors by sample",
            notes = "")
    public String getSampleErrors() {
        StopWatch stopWatch = new StopWatch();
        DomainDAO dao = DomainDAOManager.getInstance().getDao();
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
        } finally {
            activityLog.logRESTServiceCall(null, "GET", "/info/sample/errors", stopWatch.getTime());
        }
    }

    @GET
    @Path("/sample/search")
    @ApiOperation(value = "Searchs Samples based off search parameters",
            notes = "")
    @ApiResponses(value = {

    })
    @Produces(MediaType.APPLICATION_JSON)
    public String getSampleBySearchParameters(@QueryParam("name") final String name,
                                              @QueryParam("slideCode") final String slideCode,
                                              @QueryParam("line") final String line,
                                              @QueryParam("dataSet") final String dataSet,
                                              @QueryParam("wildcard") final Boolean wildcard) {
        StopWatch stopWatch = new StopWatch();
        DomainDAO dao = DomainDAOManager.getInstance().getDao();
        org.jongo.MongoCollection sample = dao.getCollectionByName("sample");

        List<Document> formattedResults = new ArrayList<Document>();
        try {
            MongoCursor<Sample> results;
            String query = null;
            String val = null;
            String field = null;

            if (name!=null) {
                field = "name";
                val = name;
            } else if (slideCode!=null) {
                field = "slideCode";
                val = slideCode;
            } else if (line!=null) {
                field = "line";
                val = line;
            } else if (dataSet!=null) {
                field = "dataSet";
                val = dataSet;
            }
            if (wildcard!=null && wildcard) {
                query = "{" + field + ":{$regex: \".*" + val + ".*\"}}";
            } else {
                query = "{" + field + ":\"" + val + "\"}";
            }

            results = sample.find(query).as(Sample.class);
            Map<String, Sample> sampleDistinctMap = new HashMap<>();
            while (results.hasNext()) {
                Sample result = results.next();
                if (!sampleDistinctMap.containsKey(result.getName())) {
                    sampleDistinctMap.put(result.getName(), result);
                }
            }

            for (Sample result : sampleDistinctMap.values()) {
                Document newDoc = new Document();
                ObjectMapper objectMapper = new ObjectMapper();
                if (result.getName()!=null) {
                    newDoc.put("name", result.getName());
                }
                if (result.getLine()!=null) {
                    newDoc.put("line", result.getLine());
                }
                if (result.getSlideCode() !=null) {
                    newDoc.put("slideCode", result.getSlideCode());
                }
                if (result.getEffector()!=null) {
                    newDoc.put("effector", result.getEffector());
                }
                if (result.getDataSet()!=null) {
                    newDoc.put("dataSet", result.getDataSet());
                }
                newDoc.put("status", (null==result.getStatus())?"":result.getStatus());
                HasFiles files = SampleUtils.getResult(result, ResultDescriptor.LATEST);
                if (files!=null && files instanceof SampleProcessingResult) {
                    SampleProcessingResult latestResult = (SampleProcessingResult)files;
                    String imagePath = latestResult.getFiles().get(FileType.SignalMip);
                    if (imagePath==null) {
                        imagePath = latestResult.getFiles().get(FileType.AllMip);
                    }
                    newDoc.put("defaultImage", latestResult.getFilepath() + File.separator + imagePath);
                } else {
                    Date latestDate = null;
                    Calendar latestCal = Calendar.getInstance();
                    String defaultImage= null;
                    for (ObjectiveSample objective : result.getObjectiveSamples()) {
                        Map<FileType, String> possibleAnswerImages = null;
                        if (objective.getLatestSuccessfulRun() != null &&
                                objective.getLatestSuccessfulRun().getLatestProcessingResult() != null) {
                            String filepath = objective.getLatestSuccessfulRun().getLatestProcessingResult().getFilepath();
                            SampleProcessingResult possibleAnswer = objective.getLatestSuccessfulRun().getLatestProcessingResult();

                            possibleAnswerImages = possibleAnswer.getFiles();
                            if (possibleAnswerImages != null) {
                                String fullPath = possibleAnswerImages.get(FileType.SignalMip);
                                if (fullPath==null) {
                                    fullPath = possibleAnswerImages.get(FileType.AllMip);
                                }
                                if (fullPath!=null && !fullPath.startsWith("/")) {
                                    fullPath = filepath + File.separator + fullPath;

                                    // compare SignalMips for different objectiveSamples
                                    Calendar cal1 = Calendar.getInstance();
                                    cal1.setTime(possibleAnswer.getCreationDate());
                                    if (latestDate == null || cal1.after(latestCal)) {
                                        latestDate = possibleAnswer.getCreationDate();
                                        latestCal.setTime(latestDate);
                                        defaultImage = fullPath;
                                    }
                                }
                            }
                        }
                    }
                    if (defaultImage!=null) {
                        newDoc.put("defaultImage", defaultImage);
                    }
                }

                formattedResults.add(newDoc);
            }
            ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            return objectMapper.writeValueAsString(formattedResults);
        } catch (Exception e) {
            log.error("Error occurred getting sample error counts by dataset",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            activityLog.logRESTServiceCall(null, "GET", "/info/sample/search", stopWatch.getTime());
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
        StopWatch stopWatch = new StopWatch();
        DomainDAO dao = DomainDAOManager.getInstance().getDao();
        MongoClient m = dao.getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        MongoCollection<Document> sample = db.getCollection("sample");
        List<Document> jsonResult = new ArrayList<>();
        try {
            if (startDate!=null || endDate!=null) {
                if (startDate==null) {
                    Date endDateTime = DateUtil.createEndDate(endDate);
                    jsonResult = sample.find(lte("tmogDate", endDateTime))
                            .batchSize(1000000)
                            .projection(fields(include("name", "line", "slideCode", "status")))
                            .into(new ArrayList());
                } else if (endDate==null) {
                    Date startDateTime = DateUtil.createStartDate(startDate);
                    jsonResult = sample.find(gte("tmogDate", startDateTime))
                            .batchSize(1000000)
                            .projection(fields(include("name", "line", "slideCode", "status")))
                            .into(new ArrayList());
                } else {
                    Date endDateTime = DateUtil.createEndDate(endDate);
                    Date startDateTime = DateUtil.createStartDate(startDate);
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
        } finally {
            activityLog.logRESTServiceCall(null, "GET", "/info/sample/workstationstatus", stopWatch.getTime());
        }
    }
}