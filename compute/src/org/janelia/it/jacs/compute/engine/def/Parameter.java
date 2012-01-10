
package org.janelia.it.jacs.compute.engine.def;

import java.io.Serializable;

/**
 * This class represents the input and output parameters of operation definitions
 *
 * @author Tareq Nabeel
 */
public class Parameter implements Serializable {

    /**
     * The name specified in the input/ouput definition.  This will be the string that will be
     * looked for in process data
     */
    private String name;

    /**
     * If specified, this value will override runtime values of <code>name</code> in process data
     */
    private Object value;

    /**
     * Whether or not this parameter is mandatory
     */
    private boolean mandatory;

    /**
     * input or output
     */
    private ParameterType parameterType;

    protected Parameter(String name, Object value, ParameterType parameterType, boolean isMandatory) {
        //this.name = Pattern.compile(patternStr);
        this.name = name;
        this.value = value;
        this.parameterType = parameterType;
        this.mandatory = isMandatory;
    }

    /**
     * Returns the name specified in the input/ouput definition.  This will be the string that will be
     * looked for in process data
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the value that will override runtime values of <code>name</code> in process data
     *
     * @return the overriding value
     */
    public Object getValue() {
        return value;
    }

    protected void setValue(Object value) {
        this.value = value;
    }

    /**
     * Returns whether or not this parameter is mandatory.  MissinDataException would be thrown if it's
     * not found in process data or queue message at runtime
     *
     * @return whether or not this parameter is mandatory
     */
    public boolean isMandatory() {
        return mandatory;
    }

    /**
     * @return String representation of this parameter definition
     */
    public String toString() {
        return "Parameter: name=" + name + ", parameterType=" + parameterType + ", mandatory=" + mandatory + ", value=" + value;
    }
}
