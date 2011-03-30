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

package org.janelia.it.jacs.shared.dma.importer;

import org.janelia.it.jacs.model.genomics.Assembly;
import org.janelia.it.jacs.shared.dma.entity.SequenceInfo;

/**
 * This class is responsible for creating an Assembly object using a parsed sequence info
 * as input
 *
 * @author Tareq Nabeel
 */
public class AssemblyFactory {

    /**
     * Create and configure an Assembly object if the parsed sequence info has
     * an assembly status
     *
     * @param info the parsed sequence info entry
     * @return the Assembly object
     */
    public static Assembly createAssembly(SequenceInfo info) {
        if (info.hasAssemblyStatus()) {
            Assembly assembly = new Assembly();
            assembly.setAssemblyAcc(getAssemblyAcc(info));
            assembly.setTaxonId(info.getTaxonId());
            assembly.setStatus(info.getAssemblyStatusStr());
            assembly.setDescription(getAssemblyDescription(info));
            info.setAssemblyAcc(assembly.getAssemblyAcc());
            return assembly;
        }
        else {
            return null;
        }
    }

    private static String getAssemblyAcc(SequenceInfo sequenceInfo) {
        StringBuilder buff = new StringBuilder("NCBI_");
        switch (sequenceInfo.getAssemblyStatus()) {
            case draft:
                buff.append("GEND_");
                break;
            case finished:
                buff.append("GENF_");
                break;
            default:
                throw new IllegalArgumentException("Invalid assembly status");
        }
        buff.append(sequenceInfo.getTaxonId());
        return buff.toString();
    }

    private static String getAssemblyDescription(SequenceInfo sequenceInfo) {
        StringBuilder buff = new StringBuilder(sequenceInfo.getAssemblyStatusStr());
        buff.append(" assembly for ");
        if (sequenceInfo.getOrganism() != null) {
            buff.append(sequenceInfo.getOrganism());
        }
        else {
            buff.append(" unknown organism");
        }
        return buff.toString();
    }

}
