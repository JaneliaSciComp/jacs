package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.service.vaa3d.MergedLsmPair;

/**
 * An anatomical area within a sample.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnatomicalArea implements Serializable {

    private String name;
    private List<Long> tileIds = new ArrayList<Long>();
    private List<MergedLsmPair> mergedLsmPairs;
    private String stitchedFilename;

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

    public List<MergedLsmPair> getMergedLsmPairs() {
        return mergedLsmPairs;
    }

    public void setMergedLsmPairs(List<MergedLsmPair> mergedLsmPairs) {
        this.mergedLsmPairs = mergedLsmPairs;
    }

    public String getStitchedFilename() {
        return stitchedFilename;
    }

    public void setStitchedFilename(String stitchedFilename) {
        this.stitchedFilename = stitchedFilename;
    }

    @Override
    public String toString() {
        return "AnatomicalArea{" +
                "name='" + name + '\'' +
                ", tileIds=" + tileIds +
                '}';
    }
}
