
package org.janelia.it.jacs.model.genomics;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

public class Chromosome extends Nucleotide implements IsSerializable, Serializable {

    private String type;

    public Chromosome() {
        super(EntityTypeGenomic.CHROMOSOME);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
