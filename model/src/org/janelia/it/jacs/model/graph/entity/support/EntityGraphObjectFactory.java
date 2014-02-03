package org.janelia.it.jacs.model.graph.entity.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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
import org.janelia.it.jacs.model.graph.annotations.RelatedToVia;
import org.janelia.it.jacs.model.graph.annotations.RelationshipInitFlag;
import org.janelia.it.jacs.model.graph.annotations.RelationshipType;
import org.janelia.it.jacs.model.graph.annotations.StartNode;
import org.janelia.it.jacs.model.util.ReflectionHelper;
import org.reflections.Reflections;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * A factory which uses the Entity/EntityData model to instantiate graph objects in a domain model, and vice-versa.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityGraphObjectFactory implements GraphObjectFactory<Entity,EntityData,EntityActorPermission> {

    private static final Logger log = Logger.getLogger(EntityGraphObjectFactory.class);

    private static final String GRAPH_OBJECT_PACKAGE_NAME = "org.janelia.it.jacs.model.graph.entity";
    
    private static Reflections reflections;
    private static Map<String,Class<?>> nodeClasses = new HashMap<String,Class<?>>();
    private static Map<String,Class<?>> relationshipClasses = new HashMap<String,Class<?>>();
    private static Map<String,Class<?>> permissionClasses = new HashMap<String,Class<?>>();
    
    static {
        reflections = new Reflections(GRAPH_OBJECT_PACKAGE_NAME);
        registerAnnotatedClasses(GraphNode.class, nodeClasses);
        registerAnnotatedClasses(GraphRelationship.class, relationshipClasses);
        registerAnnotatedClasses(GraphPermission.class, permissionClasses);
    }
    
    private Cache<Long,Object> nodeCache;
    private Cache<Long,Object> relationshipCache;
    private Cache<Long,Object> permissionCache;
    
    public EntityGraphObjectFactory() {
        this.nodeCache = CacheBuilder.newBuilder().maximumSize(10000).softValues().build();
        this.relationshipCache = CacheBuilder.newBuilder().maximumSize(10000).softValues().build();
        this.permissionCache = CacheBuilder.newBuilder().maximumSize(10000).softValues().build();
    }
    
    /**
     * Find and register classes annotated with the given annotation. We assume that the annotation has an optional
     * parameter called "type" which is used to build the type->class mapping.  
     * @param annotationClass
     * @param classMap
     */
    private static void registerAnnotatedClasses(Class<? extends Annotation> annotationClass, Map<String,Class<?>> classMap) {
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
    
    public Object initNodeInstance(Object nodeObject, Entity entity) throws Exception {

        if (!Hibernate.isInitialized(entity)) {
            return nodeObject;
        }
        
        if ((Boolean)ReflectionHelper.getMandatoryFieldValue(nodeObject, RelationshipInitFlag.class)) {
            return nodeObject;
        }
        
        // Copy permissions
        
        if (Hibernate.isInitialized(entity.getEntityActorPermissions())) {

            //log.info("  Adding permissions for entity "+entity.getId());
            
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
            
            //log.info("  Adding relationships for entity "+entity.getId());
            
            List<Object> relationshipList = new ArrayList<Object>();
            for(EntityData ed : entity.getEntityData()) {
                Entity childEntity = ed.getChildEntity();
                if (childEntity!=null) {
                    Object relObject = getRelationshipInstance(ed);
                    relationshipList.add(relObject);
                }
            }
            
            Multimap<Field,Object> filteredMap = HashMultimap.<Field,Object>create();
            for(Object relObject : relationshipList) {

                Object relId = ReflectionHelper.getMandatoryFieldValue(relObject, GraphId.class);
                Object relType = ReflectionHelper.getMandatoryFieldValue(relObject, RelationshipType.class);
                Object targetNodeObject = ReflectionHelper.getMandatoryFieldValue(relObject, EndNode.class);
                
                if (targetNodeObject==null) {
                    throw new IllegalStateException("Relationship has no target: "+relId);
                }
                
                // Go through each field and see if it wants this relationship
                for(Field field : ReflectionHelper.getFields(nodeObject)) {

                    RelatedTo relatedTo = field.getAnnotation(RelatedTo.class);
                    RelatedToVia relatedToVia = field.getAnnotation(RelatedToVia.class);
                    
                    if (relatedTo!=null) {
                        boolean match = true;
                        if (match && !"".equals(relatedTo.relationType())) {
                            if (!relatedTo.relationType().equals(relType)) {
                                //log.info("    Relationship types dont match: "+relatedTo.relationType()+"!="+relType);
                                match = false;
                            }
                        }
                        if (match && !"".equals(relatedTo.targetNodeType())) {
                            GraphNode graphNodeAnnotation = targetNodeObject.getClass().getAnnotation(GraphNode.class);
                            if (!relatedTo.targetNodeType().equals(graphNodeAnnotation.type())) {
                                //log.info("    Related node types dont match: "+relatedTo.targetNodeType()+"!="+graphNodeAnnotation.type());
                                match = false;
                            }
                        }
                        if (match) {
                            //log.info("      Will add relationship to "+field.getName());
                            filteredMap.put(field, targetNodeObject);
                        }
                    }
                    else if (relatedToVia!=null) {
                        boolean match = true;
                        if (match &&  !"".equals(relatedToVia.relationType())) {
                            if (!relatedToVia.relationType().equals(relType)) {
                                //log.info("    Relationship types dont match: "+relatedToVia.relationType()+"!="+relType);
                                match = false;
                            }
                        }
                        if (match) {
                            //log.info("      Will add relationship to "+field.getName());
                            filteredMap.put(field, relObject);
                        }
                    }
                }   
            }
            
            for (Field field : filteredMap.keys()) {
                //log.info("    For field "+field.getName());
                List<Object> filteredList = new ArrayList<Object>(filteredMap.get(field)); 
                if (Collection.class.isAssignableFrom(field.getType())) {
                    log.info("  * Setting "+nodeObject.getClass().getName()+"."+field.getName()+" = "+filteredList.size()+" items"); 
                    ReflectionHelper.setFieldValue(nodeObject, field, filteredList);
                }
                else {
                    if (filteredList.size()>1) {
                        log.warn("More than one matching relationship for "+nodeObject.getClass().getName()+"."+field.getName());
                    }
                    Object relatedItem = filteredList.get(0);
                    log.info("  * Setting "+nodeObject.getClass().getName()+"."+field.getName()+" = "+relatedItem); 
                    ReflectionHelper.setFieldValue(nodeObject, field, relatedItem);
                }
            }
            
            // Initialize relationship collections which were not touched above, so that nothing is null.
            for(Field field : ReflectionHelper.getFields(nodeObject)) {
                RelatedTo relatedTo = field.getAnnotation(RelatedTo.class);
                RelatedToVia relatedToVia = field.getAnnotation(RelatedToVia.class);
                if (relatedTo!=null || relatedToVia!=null) {
                    if (ReflectionHelper.getFieldValue(nodeObject, field)==null) {
                        if (Collection.class.isAssignableFrom(field.getType())) {
                            log.info("  * Setting "+nodeObject.getClass().getName()+"."+field.getName()+" = empty list"); 
                            ReflectionHelper.setFieldValue(nodeObject, field, new ArrayList<Object>());
                        }
                    }
                }
            }
            
            // All relationships were initialized
            ReflectionHelper.setMandatoryFieldValue(nodeObject, RelationshipInitFlag.class, Boolean.TRUE);
        }
        
        return nodeObject;
    }
    
    /**
     * Given an entity, create a node instance of the appropriate type. If the entity has initialized children, this
     * method will recurse appropriately, building an entire subgraph if necessary. It avoids infinite loops by caching 
     * all the nodes it creates.  
     * @param entity
     * @return
     * @throws Exception
     */
    @Override
    public Object getNodeInstance(Entity entity) throws Exception {
        
        Object nodeObject = nodeCache.getIfPresent(entity.getId());
        if (nodeObject!=null) return nodeObject;

        if (!Hibernate.isInitialized(entity)) {
            nodeObject = instantiateMappedClass(nodeClasses, "");
            log.info("Instantiated "+nodeObject.getClass().getName()+" for entity "+entity.getId());
            copyIdField(entity, nodeObject);
            return nodeObject;
        }
        else {
            nodeObject = instantiateMappedClass(nodeClasses, entity.getEntityTypeName());
            log.info("Instantiated "+nodeObject.getClass().getName()+" for entity "+entity.getId());
            copyAnnotatedFields(entity, nodeObject);
            nodeCache.put(entity.getId(), nodeObject);
            return initNodeInstance(nodeObject, entity);
        }        
    }
    
    @Override
    public Object getRelationshipInstance(EntityData entityData) throws Exception {

        Object relObject = relationshipCache.getIfPresent(entityData.getId());
        if (relObject!=null) return relObject;
        
        relObject = instantiateMappedClass(relationshipClasses, entityData.getEntityAttrName());
        log.info("Instantiated "+relObject.getClass().getName()+" for entityData "+entityData.getId());
        copyAnnotatedFields(entityData, relObject);
        relationshipCache.put(entityData.getId(), relObject);

        // It's important to set the end node first, because the start node (constructed next) may filter one of its 
        // collections based on the end node type. 
        Field endNodeField = ReflectionHelper.getField(relObject, EndNode.class);
        if (endNodeField!=null) {
            if (entityData.getChildEntity()!=null) {
                Object endNodeObject = getNodeInstance(entityData.getChildEntity());
                log.info("  * Setting end node to "+entityData.getChildEntity().getId());
                ReflectionHelper.setFieldValue(relObject, endNodeField, endNodeObject);
            }
            else {
                log.warn("EntityData has no child: "+entityData.getId());
            }
        }
        else {
            log.warn("Relationship type does not have any fields with @EndNode annotation");
        }
        
        Field startNodeField = ReflectionHelper.getField(relObject, StartNode.class);
        if (startNodeField!=null) {
            if (entityData.getParentEntity()!=null) {
                Object startNodeObject = getNodeInstance(entityData.getParentEntity());
                log.info("  * Setting start node to "+entityData.getParentEntity().getId());
                ReflectionHelper.setFieldValue(relObject, startNodeField, startNodeObject);
            }
            else {
                log.warn("EntityData has no parent: "+entityData.getId());
            }
        }
        else {
            log.warn("Relationship type does not have any fields with @StartNode annotation");
        }
        return relObject;
    }

    @Override
    public Object getPermissionInstance(EntityActorPermission eap) throws Exception {

        Object permObject = permissionCache.getIfPresent(eap.getId());
        if (permObject!=null) return permObject;
        
        permObject = instantiateMappedClass(permissionClasses, null);
        log.info("Instantiated "+permObject.getClass().getName()+" for permission "+eap.getId());
        copyAnnotatedFields(eap, permObject);
        permissionCache.put(eap.getId(), permObject);
        
        return permObject;
    }

    private void copyIdField(Object sourceObject, Object graphObject) throws Exception {
        // Copy id
        for(Field field : ReflectionHelper.getFields(graphObject, GraphId.class)) {
            Object idValue = ReflectionHelper.getUsingGetter(sourceObject, "id");        
            if (idValue==null) {
                log.warn("Source object has no id: "+sourceObject.getClass().getName());    
            }
            else {
                ReflectionHelper.setFieldValue(graphObject, field, idValue);    
            }
        }
    }
    
    private void copyAnnotatedFields(Object sourceObject, Object graphObject) throws Exception {

        copyIdField(sourceObject, graphObject);
        
        if (!Hibernate.isInitialized(sourceObject)) {
            log.info("  * Will not copy from uninitialized object "+sourceObject);
            return;
        }
        
        // Copy properties
        for(Field field : ReflectionHelper.getFields(graphObject, GraphProperty.class)) {
            String fieldName = field.getAnnotation(GraphProperty.class).value();
            Object value = ReflectionHelper.getUsingGetter(sourceObject, fieldName);
            log.info("  * Setting "+graphObject.getClass().getName()+"."+field.getName()+" = "+value); 
            ReflectionHelper.setFieldValue(graphObject, field, value);
        }

        // Copy attributes
        if (sourceObject instanceof Entity) {
            Entity entity = (Entity)sourceObject;
            if (Hibernate.isInitialized(entity.getEntityData())) {
                for(Field field : ReflectionHelper.getFields(graphObject, GraphAttribute.class)) {
                    String attributeName = field.getAnnotation(GraphAttribute.class).value();
                    Object value = entity.getValueByAttributeName(attributeName);
                    log.info("  * Setting "+graphObject.getClass().getName()+"."+field.getName()+" = "+value);
                    setFieldValueWithTypeCoercion(graphObject, field, value);
                }
                ReflectionHelper.setMandatoryFieldValue(graphObject, NodeInitFlag.class, Boolean.TRUE);
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
                if ("false".equalsIgnoreCase(value.toString())) {
                    ReflectionHelper.setFieldValue(obj, field, false);
                }
                // It's not null, and not any version of "false", so we assume it's true, kind of like JavaScript!
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
