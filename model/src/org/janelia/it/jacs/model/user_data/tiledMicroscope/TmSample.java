package org.janelia.it.jacs.model.user_data.tiledMicroscope;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

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

    public TmSample(Entity entity) throws Exception {
        setFromEntity(entity);
    }

    private void setFromEntity(Entity entity) throws Exception {
        if (!entity.getEntityTypeName().equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
            throw new Exception("Entity type must be "+EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE);
        }
        this.id=entity.getId();
        this.name=entity.getName();
        this.creationDate = entity.getCreationDate();
        this.filePath = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH).getValue();
    }

}
