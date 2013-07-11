package org.janelia.it.jacs.compute.access.neo4j;

import java.net.URI;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.neo4j.rest.*;
import org.janelia.it.jacs.compute.access.neo4j.rest.TraversalDefinition.Relation;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 * Data access to the Neo4j data store via its REST API.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Neo4jDAO {//extends AnnotationDAO {

    protected static final String SERVER_ROOT_URI = "http://rokicki-ws:7474/db/data";//SystemConfigurationProperties.getString("Neo4j.ServerURL");

    public Neo4jDAO(Logger _logger) {
//        super(_logger);
        
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
        // http://localhost:7474/db/data/node

        WebResource resource = Client.create().resource(nodeEntryPointUri);
        // POST {} to the node entry point URI
        ClientResponse response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
                .entity("{}").post(ClientResponse.class);

        final URI location = response.getLocation();
        System.out.println(String.format("POST to [%s], status code [%d], location header [%s]", nodeEntryPointUri,
                response.getStatus(), location.toString()));
        response.close();

        return location;
    }


    public void traverse(URI startNode) throws Exception {
        
        TraversalDefinition t = new TraversalDefinition();
        t.setOrder(TraversalDefinition.DEPTH_FIRST);
        t.setUniqueness(TraversalDefinition.NODE);
        t.setMaxDepth(10);
        t.addRelationship(new Relation("entity", Relation.OUT));

        // Once we have defined the parameters of our traversal, we just need to transfer it. We do this by determining the URI of the traversers for the start node, and then POST-ing the JSON representation of the traverser to it.
        URI traverserUri = new URI(startNode.toString() + "/traverse/node");
        WebResource resource = Client.create().resource(traverserUri);
        String jsonTraverserPayload = t.toJson();
        
        System.out.println("Payload:\n"+jsonTraverserPayload);
        
        ClientResponse response = resource.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .entity(jsonTraverserPayload)
                .post(ClientResponse.class);
         
//        System.out.println( String.format(
//                "POST [%s] to [%s], status code [%d], returned data: "
//                        + System.getProperty( "line.separator" ) + "%s",
//                jsonTraverserPayload, traverserUri, response.getStatus(),
//                response.getEntity(String.class)));
        
        String json = response.getEntity(String.class);
        response.close();
        
        json = "{\"data\":["+json+"]}";
        System.out.println("JSON:\n"+json);
        QueryResults gg = QueryResults.fromJson(json);

        for(List<Node> result : gg.getData()) {
            System.out.println("RESULT");
            for (Node n : result) {
                System.out.println("  "+n.getProperties().get("name"));
            }
        }
        
        
    }
    
    public void cypherQuery2() throws Exception {
        
        QueryDefinition query = new QueryDefinition("match (e:Entity) where e.entity_id=1889491941918244962 return e");
        URI cypherUri = new URI(SERVER_ROOT_URI + "/cypher");
        WebResource resource = Client.create().resource(cypherUri);
        String payload = query.toJson();
        ClientResponse response = resource.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .entity(payload)
                .post(ClientResponse.class);

        String json = response.getEntity(String.class);
//        System.out.println( String.format("POST [%s] to [%s], status code [%d], returned data: \n%s",
//                        payload, cypherUri, response.getStatus(), json));
        
        QueryResults gg = QueryResults.fromJson(json);
        
        for(List<Node> result : gg.getData()) {
            System.out.println("RESULT");
            for (Node n : result) {
                System.out.println("  "+n);
                
                traverse(new URI(n.getSelfUri()));
                
                
            }
        }
        
        response.close();
    }
    
    
    public void cypherQuery() throws Exception {
        
        QueryDefinition query = new QueryDefinition("match (e:Entity)-->child where e.entity_id=1889491941918244962 return child");
        URI cypherUri = new URI(SERVER_ROOT_URI + "/cypher");
        WebResource resource = Client.create().resource(cypherUri);
        String payload = query.toJson();
        ClientResponse response = resource.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .entity(payload)
                .post(ClientResponse.class);

        String json = response.getEntity(String.class);
//        System.out.println( String.format("POST [%s] to [%s], status code [%d], returned data: \n%s",
//                        payload, cypherUri, response.getStatus(), json));
        
        QueryResults gg = QueryResults.fromJson(json);
        
        for(List<Node> result : gg.getData()) {
            System.out.println("RESULT");
            for (Node n : result) {
                System.out.println("  "+n);
            }
        }
        
        response.close();
    }
    
    public static void main(String[] args) throws Exception {
        Neo4jDAO dao = new Neo4jDAO(Logger.getLogger(Neo4jDAO.class));
        dao.cypherQuery2();
    }
    
}