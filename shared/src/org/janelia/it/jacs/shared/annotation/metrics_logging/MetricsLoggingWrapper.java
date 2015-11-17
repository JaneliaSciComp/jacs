/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.shared.annotation.metrics_logging;

/**
 * This base class for string wrappers, makes the developer 'declare' what
 * kind of string s/he is passing.
 *
 * @author fosterl
 */
public abstract class MetricsLoggingWrapper {
    private String value;
    public MetricsLoggingWrapper(String value) {
        this.value = value;
    }
    
    @Override
    public String toString() { return value; }
    @Override
    public boolean equals(Object o) {
        return value.equals(o);
    }
    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
