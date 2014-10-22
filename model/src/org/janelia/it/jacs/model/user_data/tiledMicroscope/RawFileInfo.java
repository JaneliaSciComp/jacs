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
    private double[][] transformMatrix;
    private double[][] invertedTransform;
    private double[] scale;
    private int[] minCorner;
    private int[] extent;
    private List<Integer> queryMicroscopeCoords;
    private int[] queryViewCoords;

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

    public double[][] getTransformMatrix() {
        return transformMatrix;
    }

    public void setTransformMatrix(double[][] transformMatrix) {
        this.transformMatrix = transformMatrix;
    }

    public List<Integer> getQueryMicroscopeCoords() {
        return queryMicroscopeCoords;
    }

    public void setQueryMicroscopeCoords(List<Integer> queryMicroscopeCoords) {
        this.queryMicroscopeCoords = queryMicroscopeCoords;
    }

    public double[] getScale() {
        return scale;
    }

    public void setScale(double[] scale) {
        this.scale = scale;
    }

    /**
     * @return the minCorner
     */
    public int[] getMinCorner() {
        return minCorner;
    }

    /**
     * @param minCorner the minCorner to set
     */
    public void setMinCorner(int[] minCorner) {
        this.minCorner = minCorner;
    }

    /**
     * @return the extent
     */
    public int[] getExtent() {
        return extent;
    }

    /**
     * @param extent the extent to set
     */
    public void setExtent(int[] extent) {
        this.extent = extent;
    }

    /**
     * @return the invertedTransform
     */
    public double[][] getInvertedTransform() {
        return invertedTransform;
    }

    /**
     * @param invertedTransform the invertedTransform to set
     */
    public void setInvertedTransform(double[][] invertedTransform) {
        this.invertedTransform = invertedTransform;
    }

    /**
     * @return the queryViewCoords
     */
    public int[] getQueryViewCoords() {
        return queryViewCoords;
    }

    /**
     * @param queryViewCoords the queryViewCoords to set
     */
    public void setQueryViewCoords(int[] queryViewCoords) {
        this.queryViewCoords = queryViewCoords;
    }
}
