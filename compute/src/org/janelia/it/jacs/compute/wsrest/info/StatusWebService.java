package org.janelia.it.jacs.compute.wsrest.info;

import java.util.ArrayList;
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
import org.bson.Document;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.compute.wsrest.WebServiceContext;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.Filters.*;
import static java.util.Arrays.asList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/info")
public class StatusWebService extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(StatusWebService.class);

    @Context
    SecurityContext securityContext;

    public StatusWebService() {
        register(JacksonFeature.class);
    }

    @GET
    @Path("/sample")
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
}