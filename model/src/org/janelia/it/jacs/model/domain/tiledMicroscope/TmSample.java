package org.janelia.it.jacs.model.domain.tiledMicroscope;

import java.util.Date;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;
import org.janelia.it.jacs.model.domain.support.MongoMapped;

/**
 * Tiled microscope sample.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@MongoMapped(collectionName="tmSample",label="Tiled Microscope Sample")
public class TmSample extends AbstractDomainObject implements HasFilepath {

    private String filepath;
    private String micronToVoxMatrix;
    private String voxToMicronMatrix;

    public TmSample() {
    }

    public TmSample(Long id, String name) {
        setId(id);
        setName(name);
    }

    public TmSample(Long id, String name, Date creationDate, String filepath) {
        this(id, name);
        setCreationDate(creationDate);
        this.filepath = filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    @Override
    public String getFilepath() {
        return filepath;
    }

    public String getVoxToMicronMatrix() {
        return voxToMicronMatrix;
    }

    public void setVoxToMicronMatrix(String voxToMicronMatrix) {
        this.voxToMicronMatrix = voxToMicronMatrix;
    }

    public String getMicronToVoxMatrix() {
        return micronToVoxMatrix;
    }

    public void setMicronToVoxMatrix(String micronToVoxMatrix) {
        this.micronToVoxMatrix = micronToVoxMatrix;
    }

    @Override
    public String toString() {
        return getName();
    }
}
