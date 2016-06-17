package org.janelia.it.jacs.compute.wsrest.data;


import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.swagger.annotations.*;
import org.bson.Document;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.compute.access.domain.DomainDAL;
import org.janelia.it.jacs.compute.access.mongodb.DomainDAOManager;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.shared.utils.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.mongodb.client.model.Filters.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.*;


@Path("/data")
@Api(value = "Janelia Workstation Domain Data")
public class SampleWebService extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(SampleWebService.class);

    @Context
    SecurityContext securityContext;

    public SampleWebService() {
        register(JacksonFeature.class);
    }

    @GET
    @Path("/sample/lsms")
    @ApiOperation(value = "Gets a list of LSMImage stacks for a sample",
            notes = "Uses the sample ID"
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully got list of LSMImage stacks", response=LSMImage.class,
                    responseContainer = "List"),
            @ApiResponse( code = 500, message = "Internal Server Error getting list of LSMImage Stacks" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<LSMImage> getLsmsForSample(@ApiParam @QueryParam("subjectKey") final String subjectKey,
                                           @ApiParam @QueryParam("sampleId") final Long sampleId) {
        DomainDAL dao = DomainDAL.getInstance();
        try {
            Collection<LSMImage> lsms = dao.getLsmsBySampleId(subjectKey, sampleId);
            return new ArrayList<LSMImage>(lsms);
        } catch (Exception e) {
            log.error("Error occurred getting lsms for sample",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/sample")
    @ApiOperation(value = "Gets a List of Samples",
            notes = "Uses the Sample Id or Sample Name to retrieve a list of samples"
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully got Samples", response=Sample.class,
                    responseContainer = "List"),
            @ApiResponse( code = 500, message = "Internal Server Error getting list of Samples" )
    })
    @Produces(MediaType.APPLICATION_JSON)
    public List<Document> getSamples(@ApiParam @QueryParam("subjectKey") final String subjectKey,
                           @ApiParam @QueryParam("sampleId") final Long sampleId,
                           @ApiParam @QueryParam("name") final String name) {
        DomainDAO dao = DomainDAOManager.getInstance().getDao();
        MongoClient m = dao.getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        MongoCollection<Document> sample = db.getCollection("sample");
        MongoCollection<Document> image = db.getCollection("image");

        List<Document> formattedResults = new ArrayList<Document>();
        try {
            List<Document> results;
            List<Document> imageResults;

            if (sampleId==null) {
                results = sample.find(eq("name", name)).into(new ArrayList());

            } else {
                results = sample.find(eq("_id", sampleId)).batchSize(1000000).into(new ArrayList());
            }

            for (Document sampleDoc: results) {
                List<Document> objectiveSamples = (List<Document>) sampleDoc.get("objectiveSamples");
                for (Document objectiveSample : objectiveSamples) {
                    List<Document> tiles = (List<Document>) objectiveSample.get("tiles");
                    for (Document tile : tiles) {
                        String[] lsmStringIds = tile.get("lsmReferences").toString().replace("[", "").replace("]", "").replace("LSMImage#", "").split(",");
                        Long[] lsmIds = new Long[lsmStringIds.length];
                        for (int i = 0; i < lsmStringIds.length; i++) {
                            lsmIds[i] = Long.parseLong(lsmStringIds[i].trim());
                        }
                        imageResults = image.find(in("_id", lsmIds)).into(new ArrayList());
                        tile.put("lsmReferences", imageResults);
                    }
                }
            }

            // fix dates
            for (Document sampleDoc: results) {
                fixDates (sampleDoc);
            }

            // fix labels
            for (Document sampleDoc: results) {
                fixImageLabels(sampleDoc);
            }

            return results;
        } catch (Exception e) {
            log.error("Error occurred getting lsms for sample",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private void fixDates (Document doc) throws Exception {
        Iterator<String> keys = doc.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            if (doc.get(key) instanceof Date) {
                doc.put(key, DateUtil.formatDate(doc.getDate(key)));
            } else if (doc.get(key) instanceof List) {
                List subList = (List) doc.get(key);
                if (subList.size()!=0 && subList.get(0) instanceof Document) {
                    List<Document> subdocs = (List<Document>) doc.get(key);
                    for (Document subdoc : subdocs) {
                        fixDates(subdoc);
                    }
                }
            } else if (doc.get(key) instanceof Document) {
                fixDates((Document) doc.get(key));
            }
        }

    }

    // wish we had groovy closures, because most of this code looks like fixDate
    private void fixImageLabels (Document doc) throws Exception {
        Iterator<String> keys = doc.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            if (key!=null & key.equals("files")) {
                Document files = (Document)doc.get(key);
                Iterator<String> imageIterator = files.keySet().iterator();
                Map<String,String> foo = new HashMap<>();
                while (imageIterator.hasNext()) {
                    String imageKey = imageIterator.next();
                    foo.put(FileType.valueOf(imageKey).getLabel(), files.getString(imageKey));
                }
                files.clear();
                files.putAll(foo);
            } else if (doc.get(key) instanceof List) {
                List subList = (List) doc.get(key);
                if (subList.size()!=0 && subList.get(0) instanceof Document) {
                    List<Document> subdocs = (List<Document>) doc.get(key);
                    for (Document subdoc : subdocs) {
                        fixImageLabels(subdoc);
                    }
                }
            } else if (doc.get(key) instanceof Document) {
                fixImageLabels((Document) doc.get(key));
            }
        }
    }

}