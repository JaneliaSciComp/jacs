package org.janelia.it.jacs.compute.largevolume;

/**
 * Created by fosterl on 9/25/14.
 */
public class RawDataHandle {
    private String basePath;
    private String relativePath;
    private Integer[] centroid;
    private Integer[] minCorner;
    private Integer[] extent;
    private Double[] transformMatrix;

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public Integer[] getCentroid() {
        return centroid;
    }

    public void setCentroid(Integer[] centroid) {
        this.centroid = centroid;
    }

    public Integer[] getMinCorner() {
        return minCorner;
    }

    public void setMinCorner(Integer[] minCorner) {
        this.minCorner = minCorner;
    }

    public Integer[] getExtent() {
        return extent;
    }

    public void setExtent(Integer[] extent) {
        this.extent = extent;
    }

    public Double[] getTransformMatrix() {
        return transformMatrix;
    }

    public void setTransformMatrix(Double[] transformMatrix) {
        this.transformMatrix = transformMatrix;
    }
}
