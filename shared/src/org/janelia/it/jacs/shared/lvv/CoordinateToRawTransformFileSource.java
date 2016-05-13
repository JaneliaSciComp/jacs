package org.janelia.it.jacs.shared.lvv;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.CoordinateToRawTransform;

/**
 * Created by murphys on 5/13/2016.
 */
public interface CoordinateToRawTransformFileSource {

    CoordinateToRawTransform getCoordToRawTransform(String filePath) throws Exception;

}
