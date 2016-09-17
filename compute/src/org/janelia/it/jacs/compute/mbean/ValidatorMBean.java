package org.janelia.it.jacs.compute.mbean;

import javax.management.MXBean;

/**
 * MBean-required artifact: the interface for validation.
 * Created by fosterl on 6/17/14.
 */
@MXBean
public interface ValidatorMBean {
    void runValidations(String user, Long guid, String label, String types, Boolean nodebug);
}
