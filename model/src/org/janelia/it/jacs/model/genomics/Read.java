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
import java.util.HashSet;
import java.util.Set;

public class Read extends Nucleotide implements Serializable, IsSerializable {

    private String traceAcc;
    private String templateAcc;
    private String sequencingDirection;
    private Integer clearRangeBegin;
    private Integer clearRangeEnd;
    private Set<Read> mates = new HashSet<Read>(0);

    public Read() {
        super(EntityType.READ);
    }

    public String getTraceAcc() {
        return traceAcc;
    }

    public void setTraceAcc(String traceAcc) {
        this.traceAcc = traceAcc;
    }

    public String getTemplateAcc() {
        return templateAcc;
    }

    public void setTemplateAcc(String templateAcc) {
        this.templateAcc = templateAcc;
    }

    public String getSequencingDirection() {
        return sequencingDirection;
    }

    public void setSequencingDirection(String sequencingDirection) {
        this.sequencingDirection = sequencingDirection;
    }

    public Integer getClearRangeBegin() {
        return clearRangeBegin;
    }

    public void setClearRangeBegin(Integer clearRangeBegin) {
        this.clearRangeBegin = clearRangeBegin;
    }

    public Integer getClearRangeBegin_oneResCoords() {
        if (clearRangeBegin == null) {
            return null;
        }
        else {
            return clearRangeBegin + 1;
        }
    }

    public Integer getClearRangeEnd() {
        return clearRangeEnd;
    }

    public void setClearRangeEnd(Integer clearRangeEnd) {
        this.clearRangeEnd = clearRangeEnd;
    }

    public Integer getClearRangeEnd_oneResCoords() {
        return clearRangeEnd;
    }

    public Set<Read> getMates() {
        return mates;
    }

    public void setMates(Set<Read> mates) {
        this.mates = mates;
    }
}
