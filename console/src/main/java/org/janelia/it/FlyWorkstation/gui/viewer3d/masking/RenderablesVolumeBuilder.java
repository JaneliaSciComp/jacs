package org.janelia.it.FlyWorkstation.gui.viewer3d.masking;

import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanDataAcceptorI;
import org.slf4j.Logger;


/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/13/13
 * Time: 4:21 PM
 *
 * This implementation of a mask builder takes renderables as its driving data.  It will accept the renderables,
 * along with their applicable chunks of data, to produce its texture data volume, in memory.
 */
public abstract class RenderablesVolumeBuilder implements MaskChanDataAcceptorI {

    protected long sx;
    protected long sy;
    protected long sz;
    protected float[] coordCoverage;

    private int nextVolumeCount = 0;
    private VolumeConsistencyChecker checker = new VolumeConsistencyChecker();

    //----------------------------------------CONFIGURATOR METHODS
    public abstract void init();

    //----------------------------------------IMPLEMENT MaskChanDataAcceptorI (partially)
    @Override
    public synchronized void setSpaceSize( long x, long y, long z, float[] coordCoverage ) {
        if ( sx == 0 ) {
            sx = x;
            sy = y;
            sz = z;
            this.coordCoverage = coordCoverage;
            checker.accumulate( ++nextVolumeCount, new Long[]{ sx, sy, sz }, null );
        }
    }

    @Override
    public void endData( Logger logger ) {
        checker.report( true, logger );
    }

    //-------------------------END:-----------IMPLEMENT MaskChanDataAcceptorI

}
