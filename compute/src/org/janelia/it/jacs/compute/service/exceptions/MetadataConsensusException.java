package org.janelia.it.jacs.compute.service.exceptions;

/**
 * Indicates a problem where the image metadata has no consensus where a consensus is expected 
 * (e.g. all images in the same slide code having the same fly line). 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MetadataConsensusException extends MetadataException {

    public MetadataConsensusException(String msg) {
        super(msg);
    }

    public MetadataConsensusException(Throwable e) {
        super(e);
    }

    public MetadataConsensusException(String msg, Throwable e) {
        super(msg, e);
    }
}
