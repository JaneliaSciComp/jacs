
package org.janelia.it.jacs.model.genomics;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: jhoover
 * Date: Dec 7, 2006
 * Time: 9:20:38 AM
 */
public class AASequence extends BioSequence implements Serializable, IsSerializable {

    /*
     * public API
     * constructors and setters
     */
    public AASequence() {
        super(SequenceType.AA);
    }

    public AASequence(String sequence) {
        super(SequenceType.AA);
        // remove blanks, tabs, Cr, Lf, and FF
        setSequence(SeqUtil.cleanSequence(sequence));
    }
}
