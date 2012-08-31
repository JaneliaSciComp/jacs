package org.janelia.it.jacs.shared.annotation;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 8/24/12
 * Time: 1:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class DataFilter {

    private String name;
    private Float min;
    private Float max;

    public DataFilter(String name, Float min, Float max) {
        this.name=name;
        this.min=min;
        this.max=max;
    }

    public DataFilter(String valueString) throws Exception {
        String[] components=valueString.split(":");
        if (!(components.length==3)) {
            throw new Exception("Could not parse constructor string="+valueString);
        }
        name=components[0];
        min=new Float(components[1]);
        max=new Float(components[2]);
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

    public String toString() {
        StringBuffer asString=new StringBuffer();
        List<Object> vars=new ArrayList<Object>();
        vars.add(name);
        vars.add(min);
        vars.add(max);
        for (int i=0;i<vars.size();i++) {
            Object o=vars.get(i);
            if (o!=null) {
                asString.append(o.toString());
            }
            if (i<(vars.size()-1)) {
                asString.append(":");
            }
        }
        return asString.toString();
    }

}
