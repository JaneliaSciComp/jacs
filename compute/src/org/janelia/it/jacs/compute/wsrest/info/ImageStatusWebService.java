package org.janelia.it.jacs.compute.wsrest.info;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.bson.Document;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.compute.access.mongodb.DomainDAOManager;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.sample.Image;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.shared.utils.DateUtil;
import org.jongo.MongoCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        MongoCollection<Document> image = getNativeCollection("image");
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
                                    new Document ("$divide", asList(
                                            new Document("$subtract", asList("$creationDate", "$tmogDate")),
                                            3600000))))))
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
        DomainDAO dao = DomainDAOManager.getInstance().getDao();
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
            DomainDAO dao = DomainDAOManager.getInstance().getDao();
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
        MongoCollection<Document> image = getNativeCollection("image");
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
                                    new Document ("$divide", asList(
                                            new Document("$subtract", asList("$creationDate", "$tmogDate")),
                                            3600000)))),
                    new Document("$sort", new Document ("tmogDate", 1))))
                    .into(new ArrayList());
            for (Document result : jsonResult) {
                result.put("tmogDate", DateUtil.formatDate(result.getDate("tmogDate")));
                result.put("creationDate", DateUtil.formatDate(result.getDate("creationDate")));
                result.put("completionDate", DateUtil.formatDate(result.getDate("completionDate")));
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
            if (startDate!=null || endDate!=null) {
                if (startDate==null) {
                    Date endDateTime = DateUtil.createEndDate(endDate);
                    jsonResult = image.aggregate(asList(
                            new Document("$match", new Document("tmogDate",new Document("$lte", endDateTime))),
                            new Document("$project",  new Document ("_id", 0)
                                    .append("name", "$name")
                                    .append("line", "$line")
                                    .append("slideCode", "$slideCode")
                                    .append("dataSet", "$dataSet")
                                    .append("tmogDate", "$tmogDate")
                                    .append("creationDate", "$creationDate")
                                    .append("completionDate", "$completionDate")
                                    .append("cycleTime",
                                            new Document ("$divide", asList(
                                                    new Document("$subtract", asList("$creationDate", "$tmogDate")),
                                                    3600000)))),
                            new Document("$sort", new Document ("tmogDate", 1))))
                            .into(new ArrayList());
                } else if (endDate==null) {
                    Date startDateTime = DateUtil.createStartDate(startDate);
                    jsonResult = image.aggregate(asList(
                            new Document("$match", new Document("tmogDate",new Document("$gte", startDateTime))),
                            new Document("$project", new Document ("_id", 0)
                                    .append("name", "$name")
                                    .append("line", "$line")
                                    .append("slideCode", "$slideCode")
                                    .append("dataSet", "$dataSet")
                                    .append("tmogDate", "$tmogDate")
                                    .append("creationDate", "$creationDate")
                                    .append("completionDate", "$completionDate")
                                    .append("cycleTime",
                                            new Document ("$divide", asList(
                                                    new Document("$subtract", asList("$creationDate", "$tmogDate")),
                                                    3600000)))),
                            new Document("$sort", new Document ("tmogDate", 1))))
                            .into(new ArrayList());
                } else {
                    Date endDateTime = DateUtil.createEndDate(endDate);
                    Date startDateTime = DateUtil.createStartDate(startDate);
                    jsonResult = image.aggregate(asList(
                            new Document("$match", new Document("tmogDate",new Document("$gte", startDateTime))),
                            new Document("$match", new Document("tmogDate",new Document("$lte", endDateTime))),
                            new Document("$project",  new Document ("_id", 0)
                                    .append("name", "$name")
                                    .append("line", "$line")
                                    .append("slideCode", "$slideCode")
                                    .append("dataSet", "$dataSet")
                                    .append("tmogDate", "$tmogDate")
                                    .append("creationDate", "$creationDate")
                                    .append("completionDate", "$completionDate")
                                    .append("cycleTime",
                                            new Document ("$divide", asList(
                                                    new Document("$subtract", asList("$creationDate", "$tmogDate")),
                                                    3600000)))),
                            new Document("$sort", new Document ("tmogDate", 1))))
                            .into(new ArrayList());
                }
            } else {
                jsonResult = image.aggregate(asList(
                        new Document("$project", new Document ("_id", 0)
                                .append("name", "$name")
                                .append("line", "$line")
                                .append("slideCode", "$slideCode")
                                .append("dataSet", "$dataSet")
                                .append("tmogDate", "$tmogDate")
                                .append("creationDate", "$creationDate")
                                .append("completionDate", "$completionDate")
                                .append("cycleTime",
                                        new Document ("$divide", asList(
                                                new Document("$subtract", asList("$creationDate", "$tmogDate")),
                                                3600000)))),
                        new Document("$sort", new Document ("tmogDate", 1))))
                        .into(new ArrayList());
            }
            for (Document result : jsonResult) {
                result.put("tmogDate", DateUtil.formatDate(result.getDate("tmogDate")));
                result.put("creationDate", DateUtil.formatDate(result.getDate("creationDate")));
                result.put("completionDate", DateUtil.formatDate(result.getDate("completionDate")));
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
            if (compareDate==null) {
                return "You need to provide the compareDate parameter for this call to work properly";
            } else {
                Date compareDateTime = DateUtil.createStartDate(compareDate);
                jsonResult = image.aggregate(asList(
                        new Document("$match", new Document("name",name)),
                        new Document("$project", new Document("completionDate", "$completionDate")
                                .append("elapsedTime",
                                        new Document("$divide", asList(
                                                new Document("$subtract", asList("$completionDate", compareDateTime)),
                                                3600000)))),
                        new Document("$sort", new Document ("completionDate", 1))))
                        .into(new ArrayList());
            }
            for (Document result : jsonResult) {
                result.put("completionDate", DateUtil.formatDate(result.getDate("completionDate")));
            }
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            return objectMapper.writeValueAsString(jsonResult);
        } catch (Exception e) {
            log.error("Error occurred getting image cycle times",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/lsmstack/images")
    @ApiOperation(value = "Gets a LsmStack's Images",
            notes = "Uses the Sample Id or Sample Name to retrieve a list of samples"
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully got Samples", response=Sample.class,
                    responseContainer = "List"),
            @ApiResponse( code = 500, message = "Internal Server Error getting list of Samples" )
    })
    @Produces(MediaType.APPLICATION_JSON)
    public LSMImage getImage(@ApiParam @QueryParam("name") final String name) {
        DomainDAO dao = DomainDAOManager.getInstance().getDao();
        org.jongo.MongoCollection image = dao.getCollectionByName("image");

        try {
            MongoCursor<LSMImage> results;

            results = image.find("{name:'" + name + "'}").as(LSMImage.class);
            if (results.hasNext()) {
                LSMImage result = results.next();
                Map labelMap = new HashMap<String, String>();
                Iterator<FileType> files = result.getFiles().keySet().iterator();
                while (files.hasNext()) {
                    FileType moo = files.next();
                    labelMap.put(moo.getLabel(), result.getFiles().get(moo));
                }
                result.setFiles(labelMap);
                return result;
            }
        } catch (Exception e) {
            log.error("Error occurred getting LSM image information",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        return null;
    }
}