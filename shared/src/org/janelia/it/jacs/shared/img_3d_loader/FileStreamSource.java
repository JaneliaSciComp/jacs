package org.janelia.it.jacs.shared.img_3d_loader;

import java.io.InputStream;

/**
 * Created by murphys on 5/18/2016.
 */
public interface FileStreamSource {

    public InputStream getStreamForFile(String filepath) throws Exception;
}
