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
import org.janelia.it.jacs.model.genomics.DefLineFormat;

import java.util.Map;

/**
 * This class is used to initialize the Base Sequence Entity from the defline
 */
abstract public class SequenceEntityDeflineInitializer {

    protected DefLineFormat deflineFormat;

    public SequenceEntityDeflineInitializer() {
        deflineFormat = new DefLineFormat();
    }

    /**
     * copies the sequence data from a source to a destination;
     * For now it seems that the method is needed only here and only because we don't have all
     * the data set properly (e.g. ORFs are marked as GenericDNA sequences therefore retrieved as DNA).
     * Moreover once we have all the data set properly the method may not even be needed since Hibernate
     * should retrieve the appropriate proxy
     *
     * @param src
     * @param dst
     */
    public void copySequence(BaseSequenceEntity src, BaseSequenceEntity dst) {
        dst.setEntityId(src.getEntityId());
        dst.setCameraAcc(src.getCameraAcc());
        dst.setExternalAcc(src.getExternalAcc());
        dst.setDefline(src.getDefline());
        dst.setSequence(src.getSequence());
    }

    public void initialize(BaseSequenceEntity entity, String defline) {
        if (defline != null && defline.length() > 0) {
            if (defline.startsWith(entity.getCameraAcc())) {
                entity.setDefline(defline);
            }
            else {
                entity.setDefline(entity.getCameraAcc() + " " + defline);
            }
            Map deflineMap = deflineFormat.parseDefline(defline);
            initialize(entity, deflineMap);
        }
        else {
            entity.setDefline(entity.getCameraAcc());
        }
    }

    public void initialize(BaseSequenceEntity entity, Map defline) {
    }

}
