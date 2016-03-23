package org.janelia.it.jacs.compute.wsrest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.bson.Document;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.compute.access.mongodb.SolrConnector;
import org.janelia.it.jacs.compute.launcher.indexing.IndexingHelper;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.shared.solr.FacetValue;
import org.janelia.it.jacs.shared.solr.SolrJsonResults;
import org.janelia.it.jacs.shared.solr.SolrParams;
import org.janelia.it.jacs.shared.solr.SolrQueryBuilder;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.Filters.*;
import static java.util.Arrays.asList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class DataViewsWebService extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(DataViewsWebService.class);

    @Context
    SecurityContext securityContext;

    public DataViewsWebService() {
        register(JacksonFeature.class);
    }

    @POST
    @Path("/domainobject/details")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<DomainObject> getObjectDetails(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            List<DomainObject> detailObjects = null;
            if (query.getReferences()!=null) {
                detailObjects = dao.getDomainObjects(query.getSubjectKey(), query.getReferences());
            } else if (query.getObjectIds()!=null) {
                detailObjects = dao.getDomainObjects(query.getSubjectKey(), query.getObjectType(),
                        query.getObjectIds());
            }
            return detailObjects;
        } catch (Exception e) {
            log.error("Error occurred processing Object Details ",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/domainobject/name")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<DomainObject> getObjectsByName(@QueryParam("subjectKey") final String subjectKey,
                                               @QueryParam("name") final String name,
                                               @QueryParam("domainClass") final String domainClass) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        Class clazz = DomainUtils.getObjectClassByName(domainClass);
        try {
            return dao.getDomainObjectsByName(subjectKey, clazz, name);
        } catch (Exception e) {
            log.error("Error occurred retrieving domain objects using name" + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/domainobject/reverseLookup")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<DomainObject> getObjectsByReverseRef(@QueryParam("subjectKey") final String subjectKey,
                               @QueryParam("referenceId") final Long referenceId,
                               @QueryParam("count") final Long count,
                               @QueryParam("referenceAttr") final String referenceAttr,
                               @QueryParam("referenceClass") final String referenceClass) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        ReverseReference reverseRef = new ReverseReference();
        reverseRef.setCount(count);
        reverseRef.setReferenceAttr(referenceAttr);
        reverseRef.setReferenceId(referenceId);
        reverseRef.setReferringClassName(referenceClass);
        try {
            return dao.getDomainObjects(subjectKey, reverseRef);
        } catch (Exception e) {
            log.error("Error occurred retrieving domain objects using reverse ref" + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @Path("/domainobject/references")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<Reference> getContainerReferences(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            return dao.getContainerReferences(query.getDomainObject());
        } catch (Exception e) {
            log.error("Error occurred getting treenode/objectset references",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }


    @POST
    @Path("/domainobject/remove")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void removeDomainObject(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            for (Reference objectRef : query.getReferences()) {
                // first check that it is an objectset or treeNode
                Class<? extends DomainObject> objClass = DomainUtils.getObjectClassByName(objectRef.getTargetClassName());
                if (objClass==TreeNode.class || objClass==ObjectSet.class) {
                    String subjectKey = query.getSubjectKey();
                    DomainObject domainObj = dao.getDomainObject(subjectKey, objectRef);
                    // check whether this subject has permissions to write to this object
                    if (domainObj.getWriters().contains(subjectKey)) {
                        IndexingHelper.sendRemoveFromIndexMessage(domainObj.getId());
                        dao.remove(subjectKey, domainObj);
                    }
                }

            }
        } catch (Exception e) {
            log.error("Error occurred removing object references",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @Path("/domainobject")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public DomainObject updateObjectProperty(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            DomainObject updateObj = null;
            // TO DO: add check that parameters are valid
            List<Long> objIds = query.getObjectIds();
            if (objIds!=null && objIds.size()>0) {
                updateObj = dao.updateProperty(query.getSubjectKey(), query.getObjectType(), objIds.get(0),
                        query.getPropertyName(), query.getPropertyValue());
            }
            IndexingHelper.sendReindexingMessage(updateObj);

            return updateObj;

        } catch (Exception e) {
            log.error("Error occurred processing Domain Object Update Property ",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/dataset")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<DataSet> getDataSets(@QueryParam("subjectKey") final String subjectKey) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            Collection<DataSet> dataSets = dao.getDataSets(subjectKey);
            return new ArrayList<DataSet>(dataSets);
        } catch (Exception e) {
            log.error("Error occurred getting datasets",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @PUT
    @Path("/dataset")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public DataSet createDataSet(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            DataSet newDataSet = (DataSet)dao.save(query.getSubjectKey(), query.getDomainObject());
            IndexingHelper.sendReindexingMessage(newDataSet);
            return newDataSet;
        } catch (Exception e) {
            log.error("Error occurred creating DataSet ",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @Path("/dataset")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public DataSet updateDataSet(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            DataSet updateDataSet = (DataSet)dao.save(query.getSubjectKey(), query.getDomainObject());
            IndexingHelper.sendReindexingMessage(updateDataSet);
            return updateDataSet;
        } catch (Exception e) {
            log.error("Error occurred updating data set ",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }


    @DELETE
    @Path("/dataset")
    @Consumes(MediaType.APPLICATION_JSON)
    public void removeDataSet(@QueryParam("subjectKey") final String subjectKey,
                              @QueryParam("dataSetId") final String dataSetId) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        Reference dataSetRef = new Reference (Annotation.class.getName(), new Long(dataSetId));
        try {
            DomainObject domainObj = dao.getDomainObject(subjectKey, dataSetRef);
            IndexingHelper.sendRemoveFromIndexMessage(domainObj.getId());
            dao.remove(subjectKey, domainObj);
        } catch (Exception e) {
            log.error("Error occurred removing dataset",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @PUT
    @Path("/filter")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Filter createFilter(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            Filter newFilter = (Filter)dao.save(query.getSubjectKey(), query.getDomainObject());
            IndexingHelper.sendReindexingMessage(newFilter);
            return newFilter;
        } catch (Exception e) {
            log.error("Error occurred creating Search Filter ",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @Path("/filter")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Filter updateFilter(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            Filter updateFilter = (Filter)dao.save(query.getSubjectKey(), query.getDomainObject());
            IndexingHelper.sendReindexingMessage(updateFilter);
            return updateFilter;
        } catch (Exception e) {
            log.error("Error occurred updating search filter ",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @Path("/search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public SolrJsonResults searchSolrIndices(SolrParams queryParams) {
        SolrConnector solr = WebServiceContext.getSolr();
        try {
            SolrQuery query = SolrQueryBuilder.deSerializeSolrQuery(queryParams);
            QueryResponse response = solr.search(query);
            Map<String,List<FacetValue>> facetValues = new HashMap<>();
            if (response.getFacetFields()!=null) {
                for (final FacetField ff : response.getFacetFields()) {
                    List<FacetValue> favetValues = new ArrayList<>();
                    if (ff.getValues() != null) {
                        for (final FacetField.Count count : ff.getValues()) {
                            favetValues.add(new FacetValue(count.getName(), count.getCount()));
                        }
                    }
                    facetValues.put(ff.getName(), favetValues);
                }
            }

            SolrDocumentList results = response.getResults();
            log.debug("searchSolrIndices called with {} and found {} results",queryParams,results.getNumFound());
            // TODO: why this reverse? 
//            if (query.getSortField()!=null && query.getSortField().endsWith("asc")) {
//                Collections.reverse(results);
//            }
            return new SolrJsonResults(results, facetValues, response.getResults().getNumFound());

        } catch (Exception e) {
            log.error("Error occurred executing search against SOLR",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }


    @GET
    @Path("/sample/lsms")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<LSMImage> getLsmsForSample(@QueryParam("subjectKey") final String subjectKey,
                                           @QueryParam("sampleId") final Long sampleId) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            Collection<LSMImage> lsms = dao.getLsmsBySampleId(subjectKey, sampleId);
            return new ArrayList<LSMImage>(lsms);
        } catch (Exception e) {
            log.error("Error occurred getting lsms for sample",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/sample/info")
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