package org.janelia.it.jacs.model.graph.entity.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityActorPermission;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.graph.annotations.EndNode;
import org.janelia.it.jacs.model.graph.annotations.GraphAttribute;
import org.janelia.it.jacs.model.graph.annotations.GraphId;
import org.janelia.it.jacs.model.graph.annotations.GraphNode;
import org.janelia.it.jacs.model.graph.annotations.GraphPermission;
import org.janelia.it.jacs.model.graph.annotations.GraphProperty;
import org.janelia.it.jacs.model.graph.annotations.GraphRelationship;
import org.janelia.it.jacs.model.graph.annotations.NodeInitFlag;
import org.janelia.it.jacs.model.graph.annotations.Permissions;
import org.janelia.it.jacs.model.graph.annotations.RelatedTo;
import org.janelia.it.jacs.model.graph.annotations.RelationshipInitFlag;
import org.janelia.it.jacs.model.graph.annotations.RelationshipType;
import org.janelia.it.jacs.model.graph.annotations.StartNode;
import org.janelia.it.jacs.model.graph.entity.EntityNode;
import org.janelia.it.jacs.model.util.ReflectionHelper;
import org.reflections.Reflections;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * A factory which uses the Entity/EntityData model to instantiate graph objects in a domain model, and vice-versa.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityGraphObjectFactory {

    private static final Logger log = Logger.getLogger(EntityGraphObjectFactory.class);

    private static final String GRAPH_OBJECT_PACKAGE_NAME = "org.janelia.it.jacs.model.graph.entity";
    
    private Reflections reflections;
    
    private Map<String,Class<?>> nodeClasses = new HashMap<String,Class<?>>();
    private Map<String,Class<?>> relationshipClasses = new HashMap<String,Class<?>>();
    private Map<String,Class<?>> permissionClasses = new HashMap<String,Class<?>>();
    
    // TODO: this should be a singleton cache
    private Cache<Long,Object> nodeCache;
    
    public EntityGraphObjectFactory() {
        this.nodeCache = CacheBuilder.newBuilder().maximumSize(10000).softValues().build();
        this.reflections = new Reflections(GRAPH_OBJECT_PACKAGE_NAME);
        registerAnnotatedClasses(GraphNode.class, nodeClasses);
        registerAnnotatedClasses(GraphRelationship.class, relationshipClasses);
        registerAnnotatedClasses(GraphPermission.class, permissionClasses);
    }
    
    private void registerAnnotatedClasses(Class<? extends Annotation> annotationClass, Map<String,Class<?>> classMap) {
        for(Class<?> nodeClass : reflections.getTypesAnnotatedWith(annotationClass)) {
            Annotation annotation = nodeClass.getAnnotation(annotationClass);
            try {
                String type = (String)annotationClass.getMethod("type").invoke(annotation);
                if (classMap.containsKey(type)) {
                    log.warn("Overridding existing class mapping ("+classMap.get(type).getName()+") for entity type '"+type+"'");
                }
                log.info("Registering "+nodeClass.getName()+" as mapped class for type '"+type+"'");
                classMap.put(type, nodeClass);    
            }
            catch (Exception e) {
                log.error("Error retrieving 'type()' method on annotation class "+annotationClass.getName(),e);
            }
        }   
    }

    public Object getNodeInstance(Entity entity) throws Exception {
        Object nodeObject = nodeCache.getIfPresent(entity.getId());
        if (nodeObject!=null) {
            return nodeObject;
        }
        nodeObject = instantiateMappedClass(nodeClasses, entity.getEntityTypeName());
        log.info("Instantiated "+nodeObject.getClass().getName()+" for entity "+entity.getId());
        copyAnnotatedFields(entity, nodeObject);
        nodeCache.put(entity.getId(), nodeObject);

        // Copy permissions
        
        if (Hibernate.isInitialized(entity.getEntityActorPermissions())) {

            log.info("  Adding permissions for entity "+entity.getId());
            
            List<Object> permissionList = new ArrayList<Object>();
            for(EntityActorPermission eap : entity.getEntityActorPermissions()) {
                permissionList.add(getPermissionInstance(eap));
            }
            
            // Set permissions
            for(Field field : ReflectionHelper.getFields(nodeObject, Permissions.class)) {
                ReflectionHelper.setFieldValue(nodeObject, field, permissionList);
            }
        }
        
        // Copy relationships
        
        if (Hibernate.isInitialized(entity.getEntityData())) {

            log.info("  Adding relationships for entity "+entity.getId());
            
            Field nodeInitField = ReflectionHelper.getField(nodeObject, NodeInitFlag.class);
            ReflectionHelper.setFieldValue(nodeObject, nodeInitField, Boolean.TRUE);
            Field relationInitField = ReflectionHelper.getField(nodeObject, RelationshipInitFlag.class);
            ReflectionHelper.setFieldValue(nodeObject, relationInitField, Boolean.TRUE);
            
            List<Object> relationshipList = new ArrayList<Object>();
            for(EntityData ed : entity.getEntityData()) {
                Entity childEntity = ed.getChildEntity();
                if (childEntity!=null) {
                    Object relObject = getRelationshipInstance(ed);
                    relationshipList.add(relObject);
                }
            }
            
            // Set relationships
            for(Field field : ReflectionHelper.getFields(nodeObject, RelatedTo.class)) {
                
                RelatedTo relatedTo = field.getAnnotation(RelatedTo.class);
                
                List<Object> filteredList = new ArrayList<Object>();
                for(Object object : relationshipList) {
                    boolean match = true;
                    
                    if (relatedTo.relationType()!=null) {
                        Field relationTypeField = ReflectionHelper.getField(object, RelationshipType.class);
                        String relType = ReflectionHelper.getFieldValue(object, relationTypeField).toString();
                        if (!relatedTo.relationType().equals(relType)) {
                            match = false;
                        }
                    }

                    if (relatedTo.targetNodeType()!=null) {
                        Field endNodeField = ReflectionHelper.getField(object, EndNode.class);
                        Object targetNodeObject = ReflectionHelper.getFieldValue(object, endNodeField);
                        GraphNode graphNodeAnnotation = targetNodeObject.getClass().getAnnotation(GraphNode.class);
                        if (!relatedTo.targetNodeType().equals(graphNodeAnnotation.type())) {
                            match = false;
                        }
                    }
                    
                    if (match) {
                        filteredList.add(object);
                    }
                }
                
                if (Collection.class.isAssignableFrom(field.getType())) {
                    ReflectionHelper.setFieldValue(nodeObject, field, filteredList);
                }
                else {
                    if (!filteredList.isEmpty()) {
                        if (filteredList.size()>1) {
                            log.warn("More than one matching relationship for "+nodeObject.getClass().getName()+"."+field.getName());
                        }
                        ReflectionHelper.setFieldValue(nodeObject, field, filteredList.get(0));
                    }
                }
            }
        }
        
        return nodeObject;
    }
    
    public Object getRelationshipInstance(EntityData entityData) throws Exception {
        Object relObject = instantiateMappedClass(relationshipClasses, entityData.getEntityAttrName());
        log.info("Instantiated "+relObject.getClass().getName()+" for entityData "+entityData.getId());
        copyAnnotatedFields(entityData, relObject);

        Field startNodeField = ReflectionHelper.getField(relObject, StartNode.class);
        if (startNodeField!=null) {
            if (entityData.getChildEntity()!=null) {
                Object startNodeObject = getNodeInstance(entityData.getParentEntity());
                ReflectionHelper.setFieldValue(relObject, startNodeField, startNodeObject);
            }
        }
        Field endNodeField = ReflectionHelper.getField(relObject, EndNode.class);
        if (endNodeField!=null) {
            if (entityData.getChildEntity()!=null) {
                Object endNodeObject = getNodeInstance(entityData.getChildEntity());
                ReflectionHelper.setFieldValue(relObject, endNodeField, endNodeObject);
            }
        }
        
        return relObject;
    }
    
    public Object getPermissionInstance(EntityActorPermission eap) throws Exception {
        Object permObject = instantiateMappedClass(permissionClasses, null);
        log.info("Instantiated "+permObject.getClass().getName()+" for permission "+eap.getId());
        copyAnnotatedFields(eap, permObject);
        return permObject;
    }

    public String getEntityType(EntityNode entityNode) {
        GraphNode entityNodeAnnotation = entityNode.getClass().getAnnotation(GraphNode.class);
        return entityNodeAnnotation.type();
    }
    
    public Map<String,String> getAttributes(EntityNode entityNode) {
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
    
    private void copyAnnotatedFields(Object sourceObject, Object graphObject) throws Exception {

        // Copy id
        for(Field field : ReflectionHelper.getFields(graphObject, GraphId.class)) {
            ReflectionHelper.setFieldValue(graphObject, field, ReflectionHelper.getFieldValue(sourceObject, "id"));
        }
        
        // Copy properties
        for(Field field : ReflectionHelper.getFields(graphObject, GraphProperty.class)) {
            String fieldName = field.getAnnotation(GraphProperty.class).value();
            if (Hibernate.isPropertyInitialized(sourceObject, fieldName)) {
                ReflectionHelper.setFieldValue(graphObject, field, ReflectionHelper.getUsingGetter(sourceObject, fieldName));  
                log.info("  * Copying field "+sourceObject.getClass().getName()+"."+fieldName+" to "+graphObject.getClass().getName()+"."+field.getName()); 
            }
            else {
                log.info("  * Will not copy uninitialized field "+sourceObject.getClass().getName()+"."+fieldName);
            }
        }

        for(Method m : ReflectionHelper.getMethods(sourceObject, "getValueByAttributeName")) {
            StringBuilder sb = new StringBuilder();
            for(Class<?> type : m.getParameterTypes()) {
                sb.append(type+",");
            }
            log.info("  ----> "+m.getReturnType()+" "+m.getName()+"("+sb+") "+m.getClass().getName()+" in "+m.getDeclaringClass().getName());
        }

        // Copy attributes
        if (sourceObject instanceof Entity) {
            Entity entity = (Entity)sourceObject;
            if (Hibernate.isInitialized(((Entity)sourceObject).getEntityData())) {
                for(Field field : ReflectionHelper.getFields(graphObject, GraphAttribute.class)) {
                    String attributeName = field.getAnnotation(GraphAttribute.class).value();
                    Object value = entity.getValueByAttributeName(attributeName);
                    setFieldValueWithTypeCoercion(graphObject, field, value);
                    log.info("  * Copying attribute "+attributeName+" (value="+value+") to "+graphObject.getClass().getName()+"."+field.getName());
                }
            }
            else {
                log.info("  * Will not copy uninitialized attributes from entity "+sourceObject);
            }
        }
    }

    private void setFieldValueWithTypeCoercion(Object obj, Field field, Object value) {
        if (value==null) {
            ReflectionHelper.setFieldValue(obj, field, value);
            return;
        }
        Class<?> targetType = field.getType();
        if (targetType.isAssignableFrom(Integer.class)) {
            if (value instanceof Integer) {
                ReflectionHelper.setFieldValue(obj, field, value);
            }
            else {
                ReflectionHelper.setFieldValue(obj, field, Integer.parseInt(value.toString()));
            }
        }
        else if (targetType.isAssignableFrom(Float.class)) {
            if (value instanceof Float) {
                ReflectionHelper.setFieldValue(obj, field, value);
            }
            else {
                ReflectionHelper.setFieldValue(obj, field, Float.parseFloat(value.toString()));
            }
        }
        else if (targetType.isAssignableFrom(Boolean.class)) {
            if (value instanceof Boolean) {
                ReflectionHelper.setFieldValue(obj, field, value);
            }
            else {
                // True if the field is not null, and we already checked for null earlier
                ReflectionHelper.setFieldValue(obj, field, true);
            }
        }
    }
    
    private Object instantiateMappedClass(Map<String,Class<?>> classMap, String type) {

        Class<?> clazz = classMap.get(type);
        if (clazz==null) {
            clazz = classMap.get("");
            if (clazz==null) {
                throw new IllegalStateException("No registered class mapping for '"+type+"' and no default mapping is available.");
            }
        }
        
        try {
            Constructor<?> constructor = clazz.getConstructor();
            return constructor.newInstance();
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
