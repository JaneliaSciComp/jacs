package org.janelia.it.jacs.compute.access.neo4j.rest;

import java.text.ParseException;
import java.util.Date;
import java.util.Map;

import org.janelia.it.jacs.compute.access.neo4j.Neo4jBatchDAO;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;

public class EntityConverter {

    public static EntityData convertToEntityData(RelationshipResult relation, NodeResult node) {

        Map<String,String> p = relation.getProperties();
           
        EntityData entityData = new EntityData();
        entityData.setId(Long.parseLong(p.remove("entity_data_id")));
        entityData.setOwnerKey(p.remove("owner_key"));
        entityData.setOrderIndex(Integer.parseInt(p.remove("order_index")));

        EntityAttribute attrType = new EntityAttribute();
        attrType.setName(relation.getType());
        entityData.setEntityAttribute(attrType);
        
        Date updatedDate = null;
        try {
            updatedDate = Neo4jBatchDAO.df.parse(p.remove("updated_date"));
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        
        entityData.setUpdatedDate(updatedDate);
        

        Date creationDate = null;
        try {
            creationDate = Neo4jBatchDAO.df.parse(p.remove("creation_date"));
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        
        entityData.setCreationDate(creationDate);
        
        Entity childEntity = convertToEntity(node);
        entityData.setChildEntity(childEntity);
        
        return entityData;
    }
    
    public static Entity convertToEntity(NodeResult node) {
        
        Entity entity = new Entity();
        Map<String,String> p = node.getProperties();
        
        entity.setId(Long.parseLong(p.remove("entity_id")));
        entity.setName(p.remove("name"));
        entity.setOwnerKey(p.remove("owner_key"));
        
        EntityType entityType = new EntityType();
        entityType.setName(p.remove("entity_type"));
        entity.setEntityType(entityType);
        
        Date updatedDate = null;
        try {
            updatedDate = Neo4jBatchDAO.df.parse(p.remove("updated_date"));
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        
        entity.setUpdatedDate(updatedDate);
        

        Date creationDate = null;
        try {
            creationDate = Neo4jBatchDAO.df.parse(p.remove("creation_date"));
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        
        entity.setCreationDate(creationDate);
        
        for(String key : p.keySet()) {
            String value = p.get(key);
            EntityData ed = new EntityData();
            
            EntityAttribute entityAttribute = new EntityAttribute();
            entityAttribute.setName(key.toUpperCase());
            ed.setEntityAttribute(entityAttribute);
            
            ed.setValue(value);
            
            entity.getEntityData().add(ed);
        }
        
        return entity;
    }
}
