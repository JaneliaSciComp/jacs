
package org.janelia.it.jacs.model.genomics;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: jhoover
 * Date: Apr 5, 2007
 * Time: 5:23:28 PM
 */
public class Nucleotide extends BaseSequenceEntity implements Serializable, IsSerializable {

    public Nucleotide() {
        super(EntityTypeGenomic.NUCLEOTIDE);
    }

    protected Nucleotide(EntityTypeGenomic entityType) {
        super(entityType);
    }

    /*
    * sequence manipulation
    */
    public void setSequence(String sequence) {
        setBioSequence(new NASequence(sequence));
        setSequenceLength(sequence.length());
    }

    public void convertToRNA() {
        NASequence tmpRNASeq = ((NASequence) getBioSequence()).toRNA();
        setBioSequence(tmpRNASeq);
        setSequenceLength(tmpRNASeq.getLength());
    }

    public void convertToDNA() {
        NASequence tmpDNASeq = ((NASequence) getBioSequence()).toDNA();
        setBioSequence(tmpDNASeq);
        setSequenceLength(tmpDNASeq.getLength());
    }
}
