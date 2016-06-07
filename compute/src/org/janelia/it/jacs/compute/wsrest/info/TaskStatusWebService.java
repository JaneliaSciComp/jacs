package org.janelia.it.jacs.compute.wsrest.info;

import java.io.StringWriter;
import java.util.ArrayList;
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
import org.bson.Document;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.compute.access.mongodb.DomainDAOManager;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.sample.SampleProcessingResult;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.shared.utils.DateUtil;
import org.jongo.MongoCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.lte;
import static com.mongodb.client.model.Filters.not;
import static com.mongodb.client.model.Filters.regex;
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Sorts.orderBy;
import static java.util.Arrays.asList;

@Path("/info")
@Api(value = "Janelia Workstation Informatics")
public class TaskStatusWebService extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(TaskStatusWebService.class);

    @Context
    SecurityContext securityContext;

    public TaskStatusWebService() {
        register(JacksonFeature.class);
    }

    @GET
    @Path("/tasks/events")
    @ApiOperation(value = "Gets task event information using the sample entity id",
            notes = "")
    public String getTaskEvents(@QueryParam("sampleId") final String sampleId) {
        DomainDAO dao = DomainDAOManager.getInstance().getDao();
        MongoClient m = dao.getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        MongoCollection<Document> tasks =  db.getCollection("tasks");
        List<Document> jsonResult = new ArrayList<>();
        try {
            jsonResult = tasks.aggregate(asList(
                    new Document("$match", new Document("parameters.sample entity id", sampleId)),
                    new Document("$unwind", "$events"),
                    new Document("$project", new Document("jobName", "$jobName")
                            .append("_id", 0)
                            .append("eventType", "$events.eventType")
                            .append("description", "$events.description")
                            .append("timestamp", "$events.eventTimestamp")),
                    new Document("$sort", new Document("timestamp", 1))))
                    .into(new ArrayList());
            for (Document result : jsonResult) {
                result.put("timestamp", DateUtil.formatDate(result.getDate("timestamp")));
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
    @Path("/tasks/latest")
    @ApiOperation(value = "Gets tasks using a start date cutoff",
            notes = "")
    public String getLatestTasknfo(@QueryParam("startDate") final String startDate) {
        DomainDAO dao = DomainDAOManager.getInstance().getDao();
        MongoClient m = dao.getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        MongoCollection<Document> tasks =  db.getCollection("tasks");
        org.jongo.MongoCollection sample = dao.getCollectionByName("sample");
        List<Document> jsonResult = new ArrayList<>();
        try {
            if (startDate==null) {
                return "Start date is a required parameter to limit the number of tasks returned";
            }

            // filter out results by start date time
            Date startDateTime = DateUtil.createStartDate(startDate);
            jsonResult = tasks.aggregate(asList(
                    new Document("$match", new Document("events.eventTimestamp", new Document("$gte", startDateTime))),
                    new Document("$unwind", "$events"),
                    new Document("$project", new Document("sampleId", "$parameters.sample entity id")
                            .append("eventType", "$events.eventType")
                            .append("description", "$events.description")
                            .append("timestamp", "$events.eventTimestamp")
                            .append("taskTime",
                                    new Document("$subtract", asList(new Date(), "$events.eventTimestamp"))))
                   ))
                    .batchSize(500000)
                    .into(new ArrayList());
            // lookup Sample
            for (Document result: jsonResult) {
                MongoCursor<Sample> results;
                results = sample.find("{_id:'" + result.getString("sampleId") + "'}").as(Sample.class);
                if (results.hasNext()) {
                    Sample sampleInfo = results.next();
                    result.put("dataSet", sampleInfo.getDataSet());
                    result.put("sampleName", sampleInfo.getName());
                    result.put("timestamp", DateUtil.formatDate(result.getDate("timestamp")));
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


}