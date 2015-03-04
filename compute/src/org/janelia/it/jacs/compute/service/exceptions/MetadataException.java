package org.janelia.it.jacs.compute.service.exceptions;

import org.janelia.it.jacs.compute.api.ComputeException;

/**
 * Indicates a problem with metadata. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MetadataException extends ComputeException {

    public MetadataException(String msg) {
        super(msg);
    }

    public MetadataException(Throwable e) {
        super(e);
    }

    public MetadataException(String msg, Throwable e) {
        super(msg, e);
    }
}
