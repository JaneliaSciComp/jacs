
package org.janelia.it.jacs.model.genomics;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: jhoover
 * Date: Nov 9, 2006
 * Time: 11:50:10 AM
 */
public class Peptide extends BaseSequenceEntity implements IsSerializable, Serializable {

    public Peptide() {
        super(EntityTypeGenomic.PEPTIDE);
    }

    protected Peptide(EntityTypeGenomic entityType) {
        super(entityType);
    }

    /*
    * sequence manipulation
    */
    public void setSequence(String sequence) {
        AASequence tmpAASeq = new AASequence(sequence);
        setBioSequence(tmpAASeq);
        setSequenceLength(tmpAASeq.getLength());
    }
/*

    public void setAASequence(AASequence aaSequence) {
        setBioSequence(aaSequence);
    }

    public AASequence getAASequence() {
        return (AASequence) getBioSequence();
    }
*/
}
