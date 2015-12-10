package org.janelia.it.jacs.model.user_data.tiledMicroscope;

import com.google.gwt.user.client.rpc.IsSerializable;

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
    Map<String,String> propertyMap=new HashMap<>();

    public TmPreferences(Long id) {
        this.id=id;
    }

    Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }

    public void setProperty(String key, String value) {
        propertyMap.put(key, value);
    }

    public String getProperty(String key) {
        return propertyMap.get(key);
    }

    public TmPreferences() {}

}
