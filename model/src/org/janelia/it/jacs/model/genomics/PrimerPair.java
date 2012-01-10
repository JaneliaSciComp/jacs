
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
