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

    public enum DataType {
        DISCRETE,
        CONTINUOUS
    }

    private String name;
    private double min;
    private double max;
    private DataType dataType;

    public DataDescriptor(String name, double min, double max, DataType dataType) {
        this.name = name;
        this.min = min;
        this.max = max;
        this.dataType = dataType;
    }

    public DataType getDataType() {

        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }
}
