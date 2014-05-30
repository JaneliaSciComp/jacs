
package org.janelia.it.jacs.compute.engine.launcher.ejb;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.def.SeriesDef;
import org.janelia.it.jacs.compute.engine.launcher.ILauncher;
import org.janelia.it.jacs.compute.engine.launcher.LauncherException;
import org.janelia.it.jacs.compute.engine.launcher.ProcessLauncher;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;
import org.jboss.ejb3.StrictMaxPool;

import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * This Stateless Session bean wraps a ProcessLauncher and is used to demarcate the transaction boundary for all actions
 * within the process launched by it's Process Launcher.  This SLSB runs with "RequiresNew" transaction setting making it
 * run within a different transaction context than it's parent (could be ProcessLauncherSLSB if this is subprocess or
 * ComputeBeanImpl for example).  Because it initiates a new transaction, it decides what to do with that transaction when
 * something goes wrong.  In this case it throws an EJBException and causes a rollback.  The parent SLSB would also
 * rollback.
 *
 * @author Tareq Nabeel
 */
@Stateless
@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
@TransactionTimeout(432000)
@PoolClass(value = StrictMaxPool.class, maxSize = 60, timeout = 10000)
public class ProcessLauncherSLSB extends SeriesLauncherSLSB {

    protected ILauncher getLauncher() {
        return new ProcessLauncher();
    }

    public void launch(SeriesDef seriesDef, IProcessData processData) throws LauncherException, ServiceException {
        try {
            getLauncher().launch(seriesDef, processData);
        }
        catch (ComputeException e) {
            throw new EJBException(e);
        }
        // No need to catch any other exception as all throwables are caught and wrapped by ProcessLauncher in
        // a LauncherException or ServiceException
    }

}