package org.janelia.it.jacs.model.graph.support;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.graph.annotations.EndNode;
import org.janelia.it.jacs.model.graph.annotations.GraphAttribute;
import org.janelia.it.jacs.model.graph.annotations.GraphId;
import org.janelia.it.jacs.model.graph.annotations.GraphNode;
import org.janelia.it.jacs.model.graph.annotations.RelatedTo;
import org.janelia.it.jacs.model.graph.annotations.RelatedToVia;
import org.janelia.it.jacs.model.graph.annotations.RelationshipInitFlag;
import org.janelia.it.jacs.model.graph.annotations.RelationshipType;
import org.janelia.it.jacs.model.util.ReflectionHelper;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Static utility methods for dealing with generic graph entities via annotations.  
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GraphUtils {

    private static final Logger log = Logger.getLogger(GraphUtils.class);

    /**
     * Get the value of the field marked with the @amp;GraphId annotation.
     * @param graphObject
     * @return
     */
    public static Long getGraphId(Object graphObject) {
        return (Long)ReflectionHelper.getMandatoryFieldValue(graphObject, GraphId.class);
    }
    
    /**
     * Returns the entity type for the given node, based on its @amp;GraphNode annotation.
     * @param nodeObject
     * @return
     */
    public static String getEntityType(Object nodeObject) {
        GraphNode entityNodeAnnotation = nodeObject.getClass().getAnnotation(GraphNode.class);
        if (entityNodeAnnotation==null) {
        	throw new IllegalArgumentException("Cannot get entity type for object not marked with @GraphNode annotation: "+nodeObject.getClass().getName());
        }
        return entityNodeAnnotation.type();
    }
    
    /**
     * Get the attribute map of all fields marked with the @amp;GraphAttribute annotation.
     * @param nodeObject
     * @return
     */
    public static Map<String,String> getAttributes(Object nodeObject) {
        Map<String,String> attrMap = new HashMap<String,String>();
        for(Field field : ReflectionHelper.getFields(nodeObject, GraphAttribute.class)) {
            String attributeName = field.getAnnotation(GraphAttribute.class).value();
            String attributevalue = null;
            Object value = ReflectionHelper.getFieldValue(nodeObject, field);
            if (value!=null) {
                attributevalue = value.toString();
            }
            attrMap.put(attributeName, attributevalue);
        }
        return attrMap;
    }
    
    public static boolean isGeneric(RelatedTo relatedTo) {
    	return relatedTo!=null && ("".equals(relatedTo.relationType()) && "".equals(relatedTo.targetNodeType()));
    }
    
    public static boolean isGeneric(RelatedToVia relatedToVia) {
    	return relatedToVia!=null && "".equals(relatedToVia.relationType());
    }
    
	/**
	 * Dynamically initialize the relationship attributes (those marked with @amp;RelatedTo and @amp;RelatedToVia annotations) 
	 * on the given object with relationships from the given collection.
	 *  
	 * @param nodeObject A node object annotated with @amp;GraphNode
	 * @param relationshipList A list of relationship objects annotated with @amp;GraphRelationship
	 * @throws Exception
	 */
    public static void initRelationships(Object nodeObject, Collection<? extends Object> relationshipList) throws Exception {

        //Object nodeId = ReflectionHelper.getMandatoryFieldValue(nodeObject, GraphId.class);
    	//log.info("  init relationships for "+nodeId);
    	
        Multimap<Field,Object> filteredMap = HashMultimap.<Field,Object>create();
        for(Object relObject : relationshipList) {

            Object relId = ReflectionHelper.getMandatoryFieldValue(relObject, GraphId.class);
            Object relType = ReflectionHelper.getMandatoryFieldValue(relObject, RelationshipType.class);
            Object targetNodeObject = ReflectionHelper.getMandatoryFieldValue(relObject, EndNode.class);
            
            if (targetNodeObject==null) {
                throw new IllegalStateException("Relationship has no target: "+relId);
            }
            
            GraphNode graphNodeAnnotation = targetNodeObject.getClass().getAnnotation(GraphNode.class);
            //Object targetNodeId = ReflectionHelper.getMandatoryFieldValue(targetNodeObject, GraphId.class);
            
            // Go through each field and see if it wants this relationship
            for(Field field : ReflectionHelper.getFields(nodeObject)) {

                RelatedTo relatedTo = field.getAnnotation(RelatedTo.class);
                RelatedToVia relatedToVia = field.getAnnotation(RelatedToVia.class);

                boolean match = true;
                
                if (relatedTo!=null) {
                	boolean isGenericField = isGeneric(relatedTo);
                	
                    if (match && !"".equals(relatedTo.relationType())) {
                        if (!relatedTo.relationType().equals(relType)) {
                            //log.info("    Relationship types dont match: "+relatedTo.relationType()+"!="+relType);
                            match = false;
                        }
                    }
                    if (match && !"".equals(relatedTo.targetNodeType())) {
                        if (!relatedTo.targetNodeType().equals(graphNodeAnnotation.type())) {
                            //log.info("    Related node types dont match: "+relatedTo.targetNodeType()+"!="+graphNodeAnnotation.type());
                            match = false;
                        }
                    }
                    
                    if (match) {
                        if (!isGenericField && "".equals(graphNodeAnnotation.type())) {
                            //log.info("      Relationship (id="+relId+") target object is generic and "+field.getName()+" is not, so it will not be added");
                        }
                        else {
                            //log.info("      Will add node (id="+targetNodeId+") to "+nodeObject.getClass().getName()+"."+field.getName());
                            filteredMap.put(field, targetNodeObject);	
                        }
                    }
                }
                else if (relatedToVia!=null) {
                    if (match &&  !"".equals(relatedToVia.relationType())) {
                        if (!relatedToVia.relationType().equals(relType)) {
                            //log.info("    Relationship types dont match: "+relatedToVia.relationType()+"!="+relType);
                            match = false;
                        }
                    }
                    
                    if (match) {
                        //log.info("      Will add relationship (id="+relId+") to "+nodeObject.getClass().getName()+"."+field.getName());
                        filteredMap.put(field, relObject);
                    }
                }
            }   
        }
        
        for (Field field : filteredMap.keySet()) {
            List<Object> filteredList = new ArrayList<Object>(filteredMap.get(field));
            if (Collection.class.isAssignableFrom(field.getType())) {
            	// Field is a collection
                //log.info("    * Setting "+nodeObject.getClass().getName()+"."+field.getName()+" = "+filteredList.size()+" items"); 
                ReflectionHelper.setFieldValue(nodeObject, field, filteredList);
            }
            else {
                if (filteredList.size()>1) {
                    //log.warn("    More than one matching relationship for "+nodeObject.getClass().getName()+"."+field.getName());
                }
                Object relatedItem = filteredList.get(0);
                //log.info("    * Setting "+nodeObject.getClass().getName()+"."+field.getName()+" = "+relatedItem); 
                ReflectionHelper.setFieldValue(nodeObject, field, relatedItem);
            }
        }
 
        // Initialize relationship collections which were not touched above, so that nothing is null once it has been initialized
        for(Field field : ReflectionHelper.getFields(nodeObject)) {
            RelatedTo relatedTo = field.getAnnotation(RelatedTo.class);
            RelatedToVia relatedToVia = field.getAnnotation(RelatedToVia.class);
            if (relatedTo!=null || relatedToVia!=null) {
                if (ReflectionHelper.getFieldValue(nodeObject, field)==null) {
                    if (Collection.class.isAssignableFrom(field.getType())) {
                        //log.info("  * Setting "+nodeObject.getClass().getName()+"."+field.getName()+" = empty list"); 
                        ReflectionHelper.setFieldValue(nodeObject, field, new ArrayList<Object>());
                    }
                }
            }
        }
        
        // All relationships were initialized
        ReflectionHelper.setMandatoryFieldValue(nodeObject, RelationshipInitFlag.class, Boolean.TRUE);
    }
	
    /**
     * Initialize the EndNode attributes on the given object's relationships, with entities from the
     * given collection.  
     * 
	 * @param nodeObject A node object annotated with @amp;GraphNode, and having initialized relationships (isRelsInit=true)
	 * @param targetNodes A list of node objects also annotated with @amp;GraphNode
     * @throws Exception
     */
    public static void initRelationshipTargets(Object nodeObject, Collection<? extends Object> targetNodes) throws Exception {

        if (!ReflectionHelper.getMandatoryBooleanValue(nodeObject, RelationshipInitFlag.class)) {
        	throw new IllegalStateException("Cannot initialize relationship targets on entity which has uninitialized relationships");
        }
        
    	Map<Long,Object> targetNodeMap = new HashMap<Long,Object>();
    	for(Object targetNode : targetNodes) {
            Long targetNodeId = (Long)ReflectionHelper.getMandatoryFieldValue(targetNode, GraphId.class);
    		targetNodeMap.put(targetNodeId, targetNode);
    	}
    	
    	for(Object relObject : getRelationships(nodeObject)) {
    		
    		Object placeholderNode = ReflectionHelper.getMandatoryFieldValue(relObject, EndNode.class);
            Object targetNodeId = ReflectionHelper.getMandatoryFieldValue(placeholderNode, GraphId.class);
    		Object endNode = targetNodeMap.get(targetNodeId);
    		if (endNode==null) {
    			Object nodeId = ReflectionHelper.getMandatoryFieldValue(nodeObject, GraphId.class);
    			throw new IllegalStateException("Related node (id="+targetNodeId+") was not provided for starting node (id="+nodeId+")");
    		}
    		
    		//log.info("Setting relationship end node to "+endNode);
    		ReflectionHelper.setMandatoryFieldValue(relObject, EndNode.class, endNode);
    	}
    	
    }
    
    /**
     * A generic way to get all the relationships for any node object, assuming that the node object 
     * defines a field with an unqualified @RelatedToVia annotation.
     * 
     * @param nodeObject
     * @return
     * @throws Exception
     */
    public static Collection<Object> getRelationships(Object nodeObject) throws Exception {

        for(Field field : ReflectionHelper.getFields(nodeObject)) {
            RelatedToVia relatedToVia = field.getAnnotation(RelatedToVia.class);
            if (relatedToVia!=null) {
                String r =relatedToVia.relationType();
                if (r==null || "".equals(r)) {
                	Object relatedTo = ReflectionHelper.getFieldValue(nodeObject, field.getName());
                	if (Collection.class.isAssignableFrom(relatedTo.getClass())) {
                		return (Collection<Object>)relatedTo;	
                	}
                }
            }
        }
        
        return null;
    }
	
}
