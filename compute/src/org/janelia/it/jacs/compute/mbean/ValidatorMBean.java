package org.janelia.it.jacs.compute.mbean;

/**
 * MBean-required artifact: the interface for validation.
 * Created by fosterl on 6/17/14.
 */
public interface ValidatorMBean {
    void runValidations(String user, Long guid, String label, Boolean nodebug);
}
