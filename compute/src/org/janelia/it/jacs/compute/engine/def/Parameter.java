/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

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

    protected void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    /**
     * @return String representation of this parameter definition
     */
    public String toString() {
        return "Parameter: name=" + name + ", parameterType=" + parameterType + ", mandatory=" + mandatory + ", value=" + value;
    }
}
