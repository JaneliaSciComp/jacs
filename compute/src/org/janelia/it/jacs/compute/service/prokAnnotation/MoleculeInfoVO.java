
package org.janelia.it.jacs.compute.service.prokAnnotation;

public class MoleculeInfoVO {
    private String filename;
    private String topology;
    private String molType;
    private long size;

    public MoleculeInfoVO(String filename, String topology, String molType, long size) {
        this.filename = filename;
        this.topology = topology;
        this.molType = molType;
        this.size = size;
    }

    public String getFilename() {
        return filename;
    }

    public String getTopology() {
        return topology;
    }

    public String getMolType() {
        return molType;
    }

    public long getSize() {
        return size;
    }
}