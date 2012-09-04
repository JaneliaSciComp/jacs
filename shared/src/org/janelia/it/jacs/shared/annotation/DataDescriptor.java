package org.janelia.it.jacs.shared.annotation;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 8/16/12
 * Time: 10:00 AM
 * To change this template use File | Settings | File Templates.
 */
public class DataDescriptor implements Serializable {

    public enum Type {
        DISCRETE,
        CONTINUOUS,
        BOOLEAN
    }

    private String name;
    private Float min;
    private Float max;
    transient private Type type;

    DataDescriptor() {
    }

    DataDescriptor(String name, float min, float max, Type type) {
        this.name=name;
        this.min=min;
        this.max=max;
        this.type=type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Float getMin() {
        return min;
    }

    public void setMin(Float min) {
        this.min = min;
    }

    public Float getMax() {
        return max;
    }

    public void setMax(Float max) {
        this.max = max;
    }

}
