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
