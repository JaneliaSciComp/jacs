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

package org.janelia.it.jacs.shared.genomics;

import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.model.genomics.Read;

import java.util.Map;

/**
 * This class is used to initialize the Base Sequence Entity from the defline
 */
public class ReadDeflineInitializer extends SequenceEntityDeflineInitializer {

    public ReadDeflineInitializer() {
        super();
    }

    public void initialize(BaseSequenceEntity entity, Map deflineMap) {
        Read read = (Read) entity;
        setReadAttributes(read, deflineMap);
    }

    /**
     * sets Read attributes
     *
     * @param read       the entity of interest
     * @param deflineMap the entity's defLine
     */
    private void setReadAttributes(Read read, Map deflineMap) {
        String clearRangeBegin = (String) deflineMap.get("clr_range_begin");
        if (clearRangeBegin != null && clearRangeBegin.length() > 0) {
            read.setClearRangeBegin(Integer.valueOf(clearRangeBegin));
        }
        String clearRangeEnd = (String) deflineMap.get("clr_range_end");
        if (clearRangeEnd != null && clearRangeEnd.length() > 0) {
            read.setClearRangeEnd(Integer.valueOf(clearRangeEnd));
        }
        String sequenceLength = (String) deflineMap.get("full_length");
        if (sequenceLength != null && sequenceLength.length() > 0) {
            read.setSequenceLength(Integer.valueOf(sequenceLength));
        }
        String sequencingDirection = (String) deflineMap.get("sequencing_direction");
        if (sequencingDirection != null && sequencingDirection.length() > 0) {
            read.setSequencingDirection(sequencingDirection);
        }
        String libraryAcc = (String) deflineMap.get("library_id");
        if (libraryAcc != null && libraryAcc.length() > 0) {
            read.setLibraryAcc(libraryAcc);
        }
        String templateAcc = (String) deflineMap.get("template_id");
        if (templateAcc != null && templateAcc.length() > 0) {
            read.setTemplateAcc(templateAcc);
        }
    }

}
