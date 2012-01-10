
package org.janelia.it.jacs.compute.engine.def;

import java.io.Serializable;

/**
 * This class represents the different types of processors that can be specified
 * by the processorType/processor attribute of process, sequence, and operation elements
 * in a process definition
 *
 * @author Tareq Nabeel
 */
public enum ProcessorType implements Serializable {
    LOCAL_SLSB, // would use a local ejb interface to the stateless session bean
    POJO,       // package qualified class name
    LOCAL_MDB,  // MDB listening on a queue on a local JMS provider
    REMOTE_SLSB, // would use a remote ejb interface to the stateless session bean
    REMOTE_MDB, // MDB listening on a queue on a remote JMS provider
    WEB_SERVICE  //
}
