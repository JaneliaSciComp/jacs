package org.janelia.it.jacs.model.user_data.tiledMicroscope;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 5/1/13
 * Time: 1:22 PM
 */
public class TmPreferences implements IsSerializable, Serializable {
    Long id;
    Map<String,String> propertyMap=new HashMap<String,String>();

    Long getId() {
        return id;
    }

    public TmPreferences(Long id) {
        this.id=id;
    }

    public void setProperty(String key, String value) {
        propertyMap.put(key, value);
    }

    public String getProperty(String key) {
        return propertyMap.get(key);
    }

    public TmPreferences() {}

    public TmPreferences(Entity entity) throws Exception {
        if (!entity.getEntityTypeName().equals(EntityConstants.TYPE_PROPERTY_SET)) {
            throw new Exception("Entity type must be " + EntityConstants.TYPE_PROPERTY_SET);
        }
        this.id=entity.getId();
        for (EntityData ed : entity.getEntityData()) {
            if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_PROPERTY)) {
                String propertyString=ed.getValue();
                int eIndex=propertyString.indexOf("=");
                String key=propertyString.substring(0,eIndex);
                String value=propertyString.substring(eIndex+1, propertyString.length());
                propertyMap.put(key, value);
            }
        }
    }

}
