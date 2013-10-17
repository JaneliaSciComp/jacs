package org.janelia.it.jacs.compute.service.entity.sample;

import org.janelia.it.jacs.model.entity.Entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * An anatomical area within a sample.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnatomicalArea implements Serializable {

    private String name;
    private List<Entity> tiles = new ArrayList<Entity>();
    private Long sampleProcessingResultId;
    private String sampleProcessingResultFilename;

    public AnatomicalArea(String name) {
        this.name = name;
    }

    public void addTile(Entity imageTile) {
        tiles.add(imageTile);
    }

    public String getName() {
        return name;
    }

    public List<Entity> getTiles() {
        return tiles;
    }

    public Long getSampleProcessingResultId() {
        return sampleProcessingResultId;
    }

    public void setSampleProcessingResultId(Long sampleProcessingResultId) {
        this.sampleProcessingResultId = sampleProcessingResultId;
    }

    public void setSampleProcessingResultFilename(String sampleProcessingResultFilename) {
        this.sampleProcessingResultFilename = sampleProcessingResultFilename;
    }

    @Override
    public String toString() {
        return "AnatomicalArea{" +
                "name='" + name + '\'' +
                ", tiles=" + tiles +
                ", sampleProcessingResultId=" + sampleProcessingResultId +
                ", sampleProcessingResultFilename='" + sampleProcessingResultFilename + '\'' +
                '}';
    }
}
