package org.janelia.it.jacs.compute.service.exceptions;

/**
 * Indicates a problem with the LSM metadata in SAGE. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SAGEMetadataException extends MetadataException {

    public SAGEMetadataException(String msg) {
        super(msg);
    }

    public SAGEMetadataException(Throwable e) {
        super(e);
    }

    public SAGEMetadataException(String msg, Throwable e) {
        super(msg, e);
    }
}
