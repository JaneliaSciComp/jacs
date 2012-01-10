
package org.janelia.it.jacs.compute.engine.launcher;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.def.SeriesDef;

/**
 * This is the super interface for all series launchers
 *
 * @author Tareq Nabeel
 */
public interface ILauncher {
    public void launch(SeriesDef seriesDef, IProcessData processData) throws ComputeException;
}
