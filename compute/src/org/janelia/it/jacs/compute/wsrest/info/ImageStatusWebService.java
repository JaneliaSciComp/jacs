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
import org.janelia.it.jacs.model.domain.sample.Image;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
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
public class ImageStatusWebService extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(ImageStatusWebService.class);

    @Context
    SecurityContext securityContext;

    public ImageStatusWebService() {
        register(JacksonFeature.class);
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

    private MongoCollection<Document> getNativeCollection(String collectionName) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        MongoClient m = dao.getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        return db.getCollection(collectionName);
    }

    @GET
    @Path("/lsmstack/name")
    @ApiOperation(value = "Gets LSM stack By Name",
            notes = "")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Image getLsmStackByName(@QueryParam("name") final String name) {
        try {
            DomainDAO dao = WebServiceContext.getDomainManager();
            List<Image> images = (List<Image>)dao.getDomainObjectsByName(null, Image.class, name);
            if (images!=null && images.size()>0) {
                return images.get(0);
            } else return null;
        } catch (Exception e) {
            log.error("Error occurred getting LSM Stack",e);
            e.printStackTrace();
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/image/samplename")
    @ApiOperation(value = "Gets image by sample name and image type",
            notes = "")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getImageBySample(@QueryParam("name") final String name,
                                  @QueryParam("type") final String type) {
        if (name==null || type==null) {
            return "Both name and type are required parameters";
        }
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
            log.error("Error occurred getting image cycle times", e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/lsmstack/cycletime")
    @ApiOperation(value = "Gets LSM stack cycletime information",
            notes = "")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getLsmStackCycleTime(@QueryParam("startDate") final String startDate,
                                       @QueryParam("endDate") final String endDate) {
        MongoCollection<Document> image = getNativeCollection("image");
        List<Document> jsonResult = new ArrayList<>();

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            if (startDate!=null || endDate!=null) {
                if (startDate==null) {
                    Date endDateTime = sdf.parse(endDate);
                    jsonResult = image.aggregate(asList(
                            new Document("$match", new Document("tmogDate",new Document("$lte", endDateTime))),
                            new Document("$project", new Document("line", "$line")
                                    .append("slideCode", "$slideCode")
                                    .append("dataSet", "$dataSet")
                                    .append("tmogDate", "$tmogDate")
                                    .append("creationDate", "$creationDate")
                                    .append("completionDate", "$completionDate")
                                    .append("cycleTime",
                                            new Document("$subtract", asList("$creationDate", "$tmogDate"))))))
                            .into(new ArrayList());
                } else if (endDate==null) {
                    Date startDateTime = sdf.parse(startDate);
                    jsonResult = image.aggregate(asList(
                            new Document("$match", new Document("tmogDate",new Document("$gte", startDateTime))),
                            new Document("$project", new Document("line", "$line")
                                    .append("slideCode", "$slideCode")
                                    .append("dataSet", "$dataSet")
                                    .append("tmogDate", "$tmogDate")
                                    .append("creationDate", "$creationDate")
                                    .append("completionDate", "$completionDate")
                                    .append("cycleTime",
                                            new Document("$subtract", asList("$creationDate", "$tmogDate"))))))
                            .into(new ArrayList());
                } else {
                    Date endDateTime = sdf.parse(endDate);
                    Date startDateTime = sdf.parse(startDate);
                    jsonResult = image.aggregate(asList(
                            new Document("$match", new Document("tmogDate",new Document("$gte", startDateTime))),
                            new Document("$match", new Document("tmogDate",new Document("$lte", endDateTime))),
                            new Document("$project", new Document("line", "$line")
                                    .append("slideCode", "$slideCode")
                                    .append("dataSet", "$dataSet")
                                    .append("tmogDate", "$tmogDate")
                                    .append("creationDate", "$creationDate")
                                    .append("completionDate", "$completionDate")
                                    .append("cycleTime",
                                            new Document("$subtract", asList("$creationDate", "$tmogDate"))))))
                            .into(new ArrayList());
                }
            } else {
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
            }
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            String images =  objectMapper.writeValueAsString(jsonResult);
            return images;
        } catch (Exception e) {
            log.error("Error occurred getting image cycle times", e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/lsmstack/elapsed")
    @ApiOperation(value = "Compare LSMStack Completion Date to a Date given Name",
            notes = "")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getLsmStackCompareTime(@QueryParam("name") final String name,
                                         @QueryParam("compareDate") final String compareDate) {
        MongoCollection<Document> image = getNativeCollection("image");
        List<Document> jsonResult = new ArrayList<>();

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            if (compareDate==null) {
                return "You need to provide the compareDate parameter for this call to work properly";
            } else {
                Date compareDateTime = sdf.parse(compareDate);
                jsonResult = image.aggregate(asList(
                        new Document("$match", new Document("name",name)),
                        new Document("$project", new Document("completionDate", "$completionDate")
                                .append("elapsedTime",
                                        new Document("$subtract", asList("$completionDate", compareDateTime))))))
                        .into(new ArrayList());
            }
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            return objectMapper.writeValueAsString(jsonResult);
        } catch (Exception e) {
            log.error("Error occurred getting image cycle times",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}