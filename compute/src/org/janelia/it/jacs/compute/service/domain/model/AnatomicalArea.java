package org.janelia.it.jacs.compute.service.domain.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.janelia.it.jacs.compute.service.vaa3d.MergedLsmPair;

/**
 * An anatomical area within a sample.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnatomicalArea implements Serializable {

    private Long sampleId;
    private String objective;
    private String name;
    private Set<String> tileNames = new HashSet<>();
    private List<MergedLsmPair> mergedLsmPairs;
    private String stitchedFilepath;

    public AnatomicalArea(Long sampleId, String objective, String anatomicalArea) {
    	this.sampleId = sampleId;
    	this.objective = objective;
        this.name = anatomicalArea;
    }

    public Long getSampleId() {
        return sampleId;
    }

    public String getObjective() {
        return objective;
    }

    public void addTileName(String imageTileId) {
        tileNames.add(imageTileId);
    }

    public String getName() {
        return name;
    }

    public Set<String> getTileNames() {
        return tileNames;
    }

    public List<MergedLsmPair> getMergedLsmPairs() {
        return mergedLsmPairs;
    }

    public void setMergedLsmPairs(List<MergedLsmPair> mergedLsmPairs) {
        this.mergedLsmPairs = mergedLsmPairs;
    }

    public String getStitchedFilepath() {
        return stitchedFilepath;
    }

    public void setStitchedFilepath(String stitchedFilepath) {
        this.stitchedFilepath = stitchedFilepath;
    }

    @Override
    public String toString() {
        return "AnatomicalArea{" +
                "name='" + name + '\'' +
                ", tiles=" + tileNames +
                ", filepath=" + stitchedFilepath +
                '}';
    }
}
