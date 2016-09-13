
package org.janelia.it.jacs.compute.engine.launcher.ejb;

import org.janelia.it.jacs.compute.engine.launcher.ILauncher;
import org.janelia.it.jacs.compute.engine.launcher.SequenceLauncher;

import org.jboss.ejb3.annotation.TransactionTimeout;


import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * This Stateless Session bean wraps a SequenceLauncher and is used to demarcate the transaction boundary for all actions
 * within the sequence launched by it's Sequence Launcher.  This SLSB runs with "Mandatory" transaction setting making it
 * run within the transaction context of it's parent (could be ProcessLauncherSLSB or ComputeBeanImpl for example).  It first
 * glance one would wonder how this SLSB can demarcate transactions if it always runs within the context of it parent.
 * On second glance, we could extend this SLSB (as BlastLauncherSLSB does) and have that SLSB run in a separate transaction
 * context. You could need to handle the exceptions appropriately as this SLSB lets the calling EJB handle the transaction.
 * <p/>
 * Rule of thumb:  Always have the initiator of transactions decide what it needs to do with the transaction in case of error.
 * <p/>
 * Short story:  Create a subclass of this launcher if you need your sequence to run in a separate transaction.
 *
 * @author Tareq Nabeel
 */
@Stateless
@TransactionAttribute(value = TransactionAttributeType.MANDATORY)
@TransactionTimeout(432000)
//@PoolClass(value  StrictMaxPool.class, maxSize = 200, timeout = 90000)
public class SequenceLauncherSLSB extends SeriesLauncherSLSB {

    protected ILauncher getLauncher() {
        return new SequenceLauncher();
    }

}
