package org.janelia.it.jacs.compute.service.entity.sample;

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
    private List<Long> tileIds = new ArrayList<Long>();
    private Long sampleProcessingResultId;
    private String sampleProcessingResultFilename;

    public AnatomicalArea(String name) {
        this.name = name;
    }

    public void addTileId(Long imageTileId) {
        tileIds.add(imageTileId);
    }

    public String getName() {
        return name;
    }

    public List<Long> getTileIds() {
        return tileIds;
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
                ", tileIds=" + tileIds +
                ", sampleProcessingResultId=" + sampleProcessingResultId +
                ", sampleProcessingResultFilename='" + sampleProcessingResultFilename + '\'' +
                '}';
    }
}
