package org.janelia.it.jacs.compute.wsrest.info;

import java.io.StringWriter;
import java.sql.Timestamp;
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
import org.hibernate.Query;
import org.hibernate.Session;
import org.janelia.it.jacs.compute.access.mongodb.DomainDAOManager;
import org.janelia.it.jacs.compute.util.ActivityLogHelper;
import org.janelia.it.jacs.compute.util.HibernateSessionUtils;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.sample.SampleProcessingResult;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.DateUtil;
import org.jongo.MongoCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.in;
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
    ActivityLogHelper activityLog = ActivityLogHelper.getInstance();

    public TaskStatusWebService() {
        register(JacksonFeature.class);
    }

    @GET
    @Path("/tasks/events")
    @ApiOperation(value = "Gets task event information using the sample entity id",
            notes = "")
    public String getTaskEvents(@QueryParam("sampleId") final String sampleId) {
        StopWatch stopWatch = new StopWatch();
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
        } finally {
            activityLog.logRESTServiceCall(null, "GET", "/info/tasks/events", stopWatch.getTime());
        }
    }

    @GET
    @Path("/tasks/latest")
    @ApiOperation(value = "Gets tasks using a start date cutoff",
            notes = "")
    public String getLatestTasknfo(@QueryParam("hours") final String hours) {
        StopWatch stopWatch = new StopWatch();
        DomainDAO dao = DomainDAOManager.getInstance().getDao();
        MongoClient m = dao.getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        MongoCollection<Document> sample = db.getCollection("sample");
        MongoCollection<Document> tasks = db.getCollection("tasks");
        List<Document> jsonResult = new ArrayList<>();
        List<Document> formattedResults = new ArrayList<>();
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, -Integer.parseInt(hours));

        try {
            if (hours==null) {
                return "Hours is required parameter to limit the number of tasks returned";
            }

            // filter out results by start date time
            jsonResult = tasks.find(gte("events.eventTimestamp", c.getTime()))
                    .batchSize(1000000)
                    .sort(orderBy(descending("events.eventTimestamp")))
                    .into(new ArrayList());
            List<Long> sampleIds = new ArrayList<>();

            for (Document result: jsonResult) {
                Map<String, Object> parameters = (Map<String,Object>)result.get("parameters");
                Long sampleId = null;
                if (parameters!=null) {
                    String sampleRef = (String)parameters.get("sample entity id");
                    if (sampleRef!=null && sampleRef!="null") {
                        sampleId = Long.parseLong(sampleRef);
                    }
                }
                if (sampleId!=null) {
                    result.put("sampleId", sampleId);
                    sampleIds.add(sampleId);
                }
            }

            // lookup Sample
            List<Document> results = sample.find(in("_id", sampleIds))
                    .batchSize(10000000)
                    .projection(fields(include("dataSet", "name")))
                    .into(new ArrayList());
            Map<Long, Document> sampleMap = new HashMap<>();
            for (Document result: results) {
                sampleMap.put(result.getLong("_id"), result);
            }

            for (Document result: jsonResult) {
                Document newDoc = new Document();
                List<Document> events = (List<Document>)result.get("events");
                Document event = events.get(events.size()-1);
                if (result.get("sampleId")!=null && sampleMap.get(result.get("sampleId"))!=null) {
                    Document sampleInfo = sampleMap.get(result.get("sampleId"));
                    newDoc.put("sampleName", sampleInfo.getString("name"));
                    newDoc.put("dataSet", sampleInfo.getString("dataSet"));
                    newDoc.put("description", event.getString("description"));
                    newDoc.put("eventType", event.getString("eventType"));
                    newDoc.put("timestamp", DateUtil.formatDate(event.getDate("eventTimestamp")));
                    c = Calendar.getInstance();
                    Calendar d = Calendar.getInstance();
                    d.setTime(event.getDate("eventTimestamp"));
                   long secs = (c.getTimeInMillis() - d.getTimeInMillis()) / 1000;
                    long age = secs / 3600;
                    newDoc.put("age", age);
                    formattedResults.add(newDoc);
                }
            }

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            return objectMapper.writeValueAsString(formattedResults);
        } catch (Exception e) {
            log.error("Error occurred getting datasets",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            activityLog.logRESTServiceCall(null, "GET", "/info/tasks/latest", stopWatch.getTime());
        }
    }


}