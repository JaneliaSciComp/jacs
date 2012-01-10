
package org.janelia.it.jacs.model.genomics;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

public class Scaffold extends Nucleotide implements IsSerializable, Serializable {

    public Scaffold() {
        super(EntityTypeGenomic.SCAFFOLD);
    }
}
