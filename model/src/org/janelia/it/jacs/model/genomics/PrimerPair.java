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

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 13, 2009
 * Time: 2:21:17 PM
 */
public class PrimerPair {
    public static final String ORGANISM_TYPE_BACTERIAL = "bacterial";
    public static final String ORGANISM_TYPE_EUKARYOTIC = "eukaryotic";
    public static final String ORGANISM_TYPE_PLASTID = "plastid";
    public static final String ORGANISM_TYPE_ARCHAEL = "archaeal";
    public static final String ORGANISM_TYPE_FUNGAL = "fungal";
    public static final String RRNA_SUBUNIT_16S = "16S";
    public static final String RRNA_SUBUNIT_18S = "18S";
    private String rrnaSubunit;
    private String organismType;
    private String primer1Id;
    private String primer2Id;
    // Because of PCR, both forward and reverse are thought of as going from 5' to 3'
    private String primer1Sequence5to3;
    private String primer2Sequence5to3;
    private String ampliconSize;
    private String intendedProject;

    public PrimerPair(String rrnaSubunit, String organismType, String primer1Id, String primer1Sequence5to3, String primer2Id,
                      String primer2Sequence5to3, String ampliconSize, String intendedProject) {
        this.rrnaSubunit = rrnaSubunit;
        this.organismType = organismType;
        this.primer1Id = primer1Id;
        this.primer2Id = primer2Id;
        this.primer1Sequence5to3 = primer1Sequence5to3;
        this.primer2Sequence5to3 = primer2Sequence5to3;
        this.ampliconSize = ampliconSize;
        this.intendedProject = intendedProject;
    }

    public String getRrnaSubunit() {
        return rrnaSubunit;
    }

    public String getOrganismType() {
        return organismType;
    }

    public String getPrimer1Id() {
        return primer1Id;
    }

    public String getPrimer1Sequence5to3() {
        return primer1Sequence5to3;
    }

    public String getPrimer2Id() {
        return primer2Id;
    }

    public String getPrimer2Sequence5to3() {
        return primer2Sequence5to3;
    }

    public String getAmpliconSize() {
        return ampliconSize;
    }

    public String getIntendedProject() {
        return intendedProject;
    }

    public String getName() {
        return primer1Id + " and " + primer2Id;
    }
}
