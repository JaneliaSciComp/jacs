package org.janelia.it.jacs.compute.access.neo4j;

import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.neo4j.rest.NodeResult;
import org.janelia.it.jacs.compute.access.neo4j.rest.QueryDefinition;
import org.janelia.it.jacs.compute.access.neo4j.rest.QueryResults;
import org.janelia.it.jacs.compute.access.neo4j.rest.RelationshipResult;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 * Data access to the Neo4j data store via its REST API.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Neo4jTestbed {

    protected static final String SERVER_ROOT_URI = "http://rokicki-ws:7474/db/data";//SystemConfigurationProperties.getString("Neo4j.ServerURL");

    protected Logger _logger;
    
    private boolean streamJson = true;
    
    public Neo4jTestbed(Logger _logger) {
        
        this._logger = _logger;
        
        // There is a Java API binding for the REST API, but it has not caught up with the latest Neo4j release 
        // (as of 2.0 milestones): https://github.com/neo4j/java-rest-binding
        // Therefore, we need to use REST manually, which is probably for the best because it allows us to be more efficient. 

        WebResource resource = Client.create().resource(SERVER_ROOT_URI);
        ClientResponse response = resource.get(ClientResponse.class);
        _logger.info(String.format("GET on [%s], status code [%d]", SERVER_ROOT_URI, response.getStatus()));
        response.close();
    }

    public URI createNode() {
        final String nodeEntryPointUri = SERVER_ROOT_URI + "node";

        WebResource resource = Client.create().resource(nodeEntryPointUri);
        ClientResponse response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
                .entity("{}").post(ClientResponse.class);

        final URI location = response.getLocation();
        _logger.info(String.format("POST to [%s], status code [%d], location header [%s]", nodeEntryPointUri,
                response.getStatus(), location.toString()));
        response.close();

        return location;
    }
//
//    public List<EntityData> getChildNodes(Long entityId) throws Exception {
//
//        long start = System.currentTimeMillis();
//
//        QueryResults results = getCypherResults(
//                "start e=node:entity(entity_id=\""+entityId+"\") match e-[r]->child return distinct r, child", 
//                RelationshipResult.class, NodeResult.class);
//
//        List<EntityData> childrenEds = new ArrayList<EntityData>();
//        
//        for(List<Object> result : results.getData()) {
//            
//            RelationshipResult relation = (RelationshipResult)result.get(0);
//            NodeResult node = (NodeResult)result.get(1);
//
//            childrenEds.add(convertToEntityData(relation, node));
//        }
//        
//        return childrenEds;
//    }
//    
//    public void printCommonRoots(String username) throws Exception {
//
//        long start = System.currentTimeMillis();
//        
//        QueryResults results = getCypherResults(
//                "start ref=node:reference(name=\"COMMON_ROOTS\") match ref-->commonRoot-->child where commonRoot.owner_key=\""+username+"\" return distinct commonRoot", 
//                NodeResult.class);
//
//        for(List<Object> result : results.getData()) {
//            StringBuilder sb = new StringBuilder(); 
//            for (int i=0; i<results.getColumns().size(); i++) {
//                String colName = results.getColumns().get(i);
//                Object obj = result.get(i);
////                if (sb.length()>0) sb.append(", ");
////                sb.append(colName+"="+obj);
//                if (obj instanceof NodeResult) {
//                    NodeResult node = (NodeResult)obj;
//                    Entity entity = convertToEntity(node);
//                    printEntityAndChildren(entity);
//                }
//                
//            }
//            System.out.println(sb.toString());
//        }
//
//        _logger.info("Read "+results.getData().size()+" results, took "+(System.currentTimeMillis()-start)+" ms");
//    }

//    public void printEntityAndChildren(Entity entity) throws Exception {
//
//        long start = System.currentTimeMillis();
//        
//        System.out.println("-----------------------------------------");
//        System.out.println("Id="+entity.getId());
//        System.out.println("Name="+entity.getName());
////        System.out.println("Created="+entity.getCreationDate());
////        System.out.println("Updated="+entity.getUpdatedDate());
//        System.out.println("EntityType="+entity.getEntityType().getName());
//        
//        List<EntityData> childrenEds = getChildNodes(entity.getId());
//        System.out.println("Children="+childrenEds.size());
//        
////        for(EntityData ed : childrenEds) {
////            System.out.println("    -----------------------------------------");
////            System.out.println("    Id="+ed.getId());
////            System.out.println("    Name="+ed.getChildEntity().getName());
////            System.out.println("    Created="+entity.getCreationDate());
////            System.out.println("    Updated="+entity.getUpdatedDate());
////            System.out.println("    EntityAttribute="+ed.getEntityAttribute().getName());    
////        }
//        
////        
////        QueryResults results = getCypherResults(
////                "start ref=node:reference(name=\"COMMON_ROOTS\") match ref-->commonRoot-->child where commonRoot.owner_key=\"user:rokickik\" return distinct commonRoot", 
////                NodeResult.class);
////
////        for(List<Object> result : results.getData()) {
////            StringBuilder sb = new StringBuilder(); 
////            for (int i=0; i<results.getColumns().size(); i++) {
////                String colName = results.getColumns().get(i);
////                Object obj = result.get(i);
////                if (sb.length()>0) sb.append(", ");
////                sb.append(colName+"="+obj);
////            }
////            _logger.info(sb);
////        }
////
////        _logger.info("Read "+results.getData().size()+" results, took "+(System.currentTimeMillis()-start)+" ms");
//    }

    public QueryResults getCypherResults(String cypherQuery, Class... resultTypes) throws Exception {
        QueryDefinition query = new QueryDefinition(cypherQuery);
        return getCypherResults(query, resultTypes);
    }
    
    public QueryResults getCypherResults(QueryDefinition query, Class... resultTypes) throws Exception {
        
        URI cypherUri = new URI(SERVER_ROOT_URI + "/cypher");
        WebResource resource = Client.create().resource(cypherUri);
        String payload = query.toJson();
        
        String mediaType = MediaType.APPLICATION_JSON;
        if (streamJson) {
            mediaType += ";stream=true";
        }
        
        ClientResponse response = resource.accept(mediaType)
                .type(MediaType.APPLICATION_JSON)
                .entity(payload)
                .post(ClientResponse.class);

        String json = response.getEntity(String.class);
        QueryResults results = QueryResults.fromJson(json, resultTypes);
        
        response.close();
        return results;
    }
    
    public List<Entity> getEntitiesByNameAndTypeName(String subjectKey, String entityName, String entityTypeName) throws DaoException {
        try {
            if (_logger.isDebugEnabled()) {
                _logger.debug("getEntitiesByNameAndTypeName(subjectKey="+subjectKey+",entityName="+entityName+",entityTypeName=entityTypeName)");
            }

            List<String> subjectKeyList = new ArrayList<String>();
            subjectKeyList.add("group:flylight");
            subjectKeyList.add(subjectKey);
            
            QueryDefinition query = new QueryDefinition(
                    "start e=node:entity(name={entityName}) where e.entity_type = {entityType} and e.username in [{subjectKeyList}] return distinct e");
            
            query.addParam("entityName", entityName);
            query.addParam("entityType", entityTypeName);
            query.addParam("subjectKeyList", subjectKeyList);
            
            QueryResults results = getCypherResults(query, NodeResult.class);
            return results.getEntityResults();
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }
    
    public void cypherQuery2() throws Exception {

        long start = System.currentTimeMillis();
        
        QueryResults results = getCypherResults(
                "start ref=node:reference(name=\"COMMON_ROOTS\") match ref-->commonRoot-->child where commonRoot.owner_key=\"user:nerna\" return commonRoot.name, count(child)", 
                String.class, Integer.class);
        
        for(List<Object> result : results.getData()) {
            StringBuilder sb = new StringBuilder(); 
            for (int i=0; i<results.getColumns().size(); i++) {
                String colName = results.getColumns().get(i);
                Object obj = result.get(i);
                if (sb.length()>0) sb.append(", ");
                sb.append(colName+"="+obj);
            }
            _logger.info(sb);
        }

        _logger.info("Read "+results.getData().size()+" results, took "+(System.currentTimeMillis()-start)+" ms");
    }
    
    public static void main(String[] args) throws Exception {
        Neo4jTestbed dao = new Neo4jTestbed(Logger.getLogger(Neo4jTestbed.class));
        for(Entity entity : dao.getEntitiesByNameAndTypeName("user:nerna", "Shared Data", "Folder")) {
            System.out.println(entity.getName()+" "+entity.getId());
        }
    }
    
}