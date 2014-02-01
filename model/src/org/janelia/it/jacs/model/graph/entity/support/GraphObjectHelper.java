package org.janelia.it.jacs.model.graph.entity.support;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.janelia.it.jacs.model.graph.annotations.GraphAttribute;
import org.janelia.it.jacs.model.graph.annotations.GraphNode;
import org.janelia.it.jacs.model.graph.entity.EntityNode;
import org.janelia.it.jacs.model.util.ReflectionHelper;

/**
 * Static helper methods for dealing with graph object annotations.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GraphObjectHelper {

    public static String getEntityType(EntityNode entityNode) {
        GraphNode entityNodeAnnotation = entityNode.getClass().getAnnotation(GraphNode.class);
        return entityNodeAnnotation.type();
    }
    
    public static Map<String,String> getAttributes(EntityNode entityNode) {
        Map<String,String> attrMap = new HashMap<String,String>();
        for(Field field : ReflectionHelper.getFields(entityNode, GraphAttribute.class)) {
            String attributeName = field.getAnnotation(GraphAttribute.class).value();
            String attributevalue = null;
            Object value = ReflectionHelper.getFieldValue(entityNode, field);
            if (value!=null) {
                attributevalue = value.toString();
            }
            attrMap.put(attributeName, attributevalue);
        }
        return attrMap;
    }
}
