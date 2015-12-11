package org.janelia.it.jacs.model.user_data.tiledMicroscope;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 5/1/13
 * Time: 1:21 PM
 */
public class TmSample implements IsSerializable, Serializable {

    Long id;
    String name;
    Date creationDate;
    String filePath;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    @Override
    public String toString() {
        return name;
    }

    public TmSample(Long id, String name) {
        this.id=id;
        this.name=name;
    }

    public TmSample(Long id, String name, Date creationDate, String filePath) {
        this.id=id;
        this.name=name;
        this.creationDate = creationDate;
        this.filePath = filePath;
    }

}
