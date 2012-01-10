
package org.janelia.it.jacs.model.genomics;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: jhoover
 * Date: Dec 7, 2006
 * Time: 9:20:38 AM
 */
public class NASequence extends BioSequence implements Serializable, IsSerializable {

    public NASequence() {
        super(SequenceType.NA);
    }

    public NASequence(String sequence) {

        super(SequenceType.NA);
        // remove blanks, tabs, Cr, Lf, and FF
        setSequence(SeqUtil.cleanSequence(sequence));
    }

    /*
    * sequence manipulation
    */
    public NASequence complement() {
        String compString = SeqUtil.convertText(
                getSequence(),
                SequenceType.NA.getElements(),
                SequenceType.NA.getComplements());
        NASequence compseq = new NASequence();
        compseq.setSequence(compString);
        return compseq;
    }

    public NASequence toRNA() {
        NASequence rnaseq = new NASequence();
        rnaseq.setSequence(getSequence().replace('T', 'U').replace('t', 'u'));
        return rnaseq;
    }

    public NASequence toDNA() {
        NASequence dnaseq = new NASequence();
        dnaseq.setSequence(getSequence().replace('U', 'T').replace('u', 't'));
        return dnaseq;
    }

    public BioSequence subSequence(int begin, int end, int ori) {
        if (ori == BioSequence.FORWARD_ORIENTATION)
            return super.subSequence(begin, end, ori);
        else
            return ((NASequence) super.subSequence(begin, end, ori)).complement();
    }
}