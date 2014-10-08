package org.janelia.it.jacs.model.user_data.tiledMicroscope;

import java.io.File;
import java.io.Serializable;
import java.util.List;

/**
 * Created by fosterl on 10/7/14.
 */
public class RawFileInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private File channel0;
    private File channel1;
    private List<Integer> centroid;
    private Double[] transformMatrix;

    public File getChannel0() {
        return channel0;
    }

    public void setChannel0(File channel0) {
        this.channel0 = channel0;
    }

    public File getChannel1() {
        return channel1;
    }

    public void setChannel1(File channel1) {
        this.channel1 = channel1;
    }

    public List<Integer> getCentroid() {
        return centroid;
    }

    public void setCentroid(List<Integer> centroid) {
        this.centroid = centroid;
    }

    public Double[] getTransformMatrix() {
        return transformMatrix;
    }

    public void setTransformMatrix(Double[] transformMatrix) {
        this.transformMatrix = transformMatrix;
    }
}
