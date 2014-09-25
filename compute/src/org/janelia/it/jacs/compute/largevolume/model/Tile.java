package org.janelia.it.jacs.compute.largevolume.model;

import java.util.List;

/**
 * Created by fosterl on 9/24/14.
 */
public class Tile {
    private String path;
    private Aabb aabb;
    private Shape shape;
    private Double[] transform;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Aabb getAabb() {
        return aabb;
    }

    public void setAabb(Aabb aabb) {
        this.aabb = aabb;
    }

    public Shape getShape() {
        return shape;
    }

    public void setShape(Shape shape) {
        this.shape = shape;
    }

    public Double[] getTransform() {
        return transform;
    }

    public void setTransform(Double[] transform) {
        this.transform = transform;
    }

}
