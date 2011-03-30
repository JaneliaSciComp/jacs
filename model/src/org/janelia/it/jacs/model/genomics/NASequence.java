/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

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