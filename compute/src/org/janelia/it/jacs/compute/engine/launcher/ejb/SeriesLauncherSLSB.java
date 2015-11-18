
package org.janelia.it.jacs.compute.engine.launcher.ejb;

import org.janelia.it.jacs.compute.access.ComputeException;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.def.SeriesDef;
import org.janelia.it.jacs.compute.engine.launcher.ILauncher;

/**
 * @author Tareq Nabeel
 */
public abstract class SeriesLauncherSLSB implements ILauncher {

    protected abstract ILauncher getLauncher();

    public void launch(SeriesDef seriesDef, IProcessData processData) throws ComputeException {
        getLauncher().launch(seriesDef, processData);
    }

}
