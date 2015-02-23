package org.janelia.it.jacs.compute.service.exceptions;

/**
 * Indicates a problem with the metadata encoded in the LSM file.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LSMMetadataException extends MetadataException {

    public LSMMetadataException(String msg) {
        super(msg);
    }

    public LSMMetadataException(Throwable e) {
        super(e);
    }

    public LSMMetadataException(String msg, Throwable e) {
        super(msg, e);
    }
}
