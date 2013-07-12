package org.janelia.it.jacs.compute.access.neo4j;

import java.net.URI;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.neo4j.rest.NodeResult;
import org.janelia.it.jacs.compute.access.neo4j.rest.QueryDefinition;
import org.janelia.it.jacs.compute.access.neo4j.rest.QueryResults;
import org.janelia.it.jacs.compute.access.neo4j.rest.RelationshipResult;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 * Data access to the Neo4j data store via its REST API.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Neo4jDAO {

    protected static final String SERVER_ROOT_URI = "http://rokicki-ws:7474/db/data";//SystemConfigurationProperties.getString("Neo4j.ServerURL");

    private boolean streamJson = true;
    
    public Neo4jDAO(Logger _logger) {
        
        // There is a Java API binding for the REST API, but it has not caught up with the latest Neo4j release 
        // (as of 2.0 milestones): https://github.com/neo4j/java-rest-binding
        // Therefore, we need to use REST manually, which is probably for the best because it allows us to be more efficient. 

        WebResource resource = Client.create().resource(SERVER_ROOT_URI);
        ClientResponse response = resource.get(ClientResponse.class);
        System.out.println(String.format("GET on [%s], status code [%d]", SERVER_ROOT_URI, response.getStatus()));
        response.close();
    }

    public URI createNode() {
        final String nodeEntryPointUri = SERVER_ROOT_URI + "node";

        WebResource resource = Client.create().resource(nodeEntryPointUri);
        ClientResponse response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
                .entity("{}").post(ClientResponse.class);

        final URI location = response.getLocation();
        System.out.println(String.format("POST to [%s], status code [%d], location header [%s]", nodeEntryPointUri,
                response.getStatus(), location.toString()));
        response.close();

        return location;
    }
    
    public QueryResults getCypherResults(String cypherQuery, Class<? extends Object>... resultTypes) throws Exception {

        long start = System.currentTimeMillis();
        
        QueryDefinition query = new QueryDefinition(cypherQuery);
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
//        System.out.println( String.format("POST [%s] to [%s], status code [%d], returned data: \n%s",
//                        payload, cypherUri, response.getStatus(), json));
//        System.out.println("Got "+json.length() +" bytes, took "+(System.currentTimeMillis()-start)+" ms");
        
        QueryResults results = QueryResults.fromJson(json, resultTypes);
//        System.out.println("Read "+gg.getData().size()+" results, took "+(System.currentTimeMillis()-start)+" ms");
        
        response.close();
        return results;
    }
    
    public void cypherQuery1() throws Exception {

        long start = System.currentTimeMillis();
        
        @SuppressWarnings("unchecked")
        QueryResults results = getCypherResults(
                "match e:Entity-[r]->child where e.entity_id=1859269906990628962 return r,child", 
                RelationshipResult.class, NodeResult.class);
        
        for(List<Object> result : results.getData()) {
            System.out.println("RESULT");
            for (Object n : result) {
                System.out.println("    "+n);
            }
        }

        System.out.println("Read "+results.getData().size()+" results, took "+(System.currentTimeMillis()-start)+" ms");
    }

    public void cypherQuery2() throws Exception {

        long start = System.currentTimeMillis();
        
        @SuppressWarnings("unchecked")
        QueryResults results = getCypherResults(
                "match e:CommonRoot-[r:entity]->child where e.owner_key=\"user:nerna\" return e.name,count(r)", 
                String.class, Integer.class);
        
        for(List<Object> result : results.getData()) {
            System.out.println("RESULT");
            for (Object n : result) {
                System.out.println("    "+n);
            }
        }

        System.out.println("Read "+results.getData().size()+" results, took "+(System.currentTimeMillis()-start)+" ms");
    }
    
    public static void main(String[] args) throws Exception {
        Neo4jDAO dao = new Neo4jDAO(Logger.getLogger(Neo4jDAO.class));
        dao.cypherQuery2();
    }
    
}