
package org.janelia.it.jacs.model.genomics;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: jhoover
 * Date: Nov 6, 2006
 * Time: 3:17:48 PM
 */
public class EntityTypeGenomic implements Serializable, IsSerializable {

    public static final int ENTITY_CODE_UNKNOWN = -1;
    public static final int ENTITY_CODE_NUCLEOTIDE = 0;
    public static final int ENTITY_CODE_CHROMOSOME = 1;
    public static final int ENTITY_CODE_SCAFFOLD = 2;
    public static final int ENTITY_CODE_READ = 3;
    public static final int ENTITY_CODE_GENE = 4;
    public static final int ENTITY_CODE_ORF = 5;
    public static final int ENTITY_CODE_PROTEIN = 6;
    public static final int ENTITY_CODE_PEPTIDE = 7;
    public static final int ENTITY_CODE_NON_CODING_RNA = 8;


    public static final EntityTypeGenomic UNKNOWN = new EntityTypeGenomic(ENTITY_CODE_UNKNOWN, "Unknown", "UNK", "", SequenceType.UNKNOWN);
    public static final EntityTypeGenomic NUCLEOTIDE = new EntityTypeGenomic(ENTITY_CODE_NUCLEOTIDE, "Nucleotide", "NUC", "", SequenceType.NA);
    public static final EntityTypeGenomic CHROMOSOME = new EntityTypeGenomic(ENTITY_CODE_CHROMOSOME, "Chromosome", "CHR", "", SequenceType.NA);
    public static final EntityTypeGenomic SCAFFOLD = new EntityTypeGenomic(ENTITY_CODE_SCAFFOLD, "Scaffold", "SCAF", "", SequenceType.NA);
    public static final EntityTypeGenomic READ = new EntityTypeGenomic(ENTITY_CODE_READ, "Read", "READ", "", SequenceType.NA);
    public static final EntityTypeGenomic GENE = new EntityTypeGenomic(ENTITY_CODE_GENE, "Gene", "Gene", "", SequenceType.NA);
    public static final EntityTypeGenomic ORF = new EntityTypeGenomic(ENTITY_CODE_ORF, "ORF", "ORF", "", SequenceType.NA);
    public static final EntityTypeGenomic PROTEIN = new EntityTypeGenomic(ENTITY_CODE_PROTEIN, "Protein", "PRO", "", SequenceType.AA);
    public static final EntityTypeGenomic PEPTIDE = new EntityTypeGenomic(ENTITY_CODE_PEPTIDE, "Peptide", "PEP", "", SequenceType.AA);
    public static final EntityTypeGenomic NON_CODING_RNA = new EntityTypeGenomic(ENTITY_CODE_NON_CODING_RNA, "ncRNA", "ncRNA", "", SequenceType.NA);


    private int code;
    private String name;
    private String abbrev;
    private String description;
    private SequenceType sequenceType;

    public EntityTypeGenomic() {
    }

    public EntityTypeGenomic(int code, String name, String abbrev, String description, SequenceType sequenceType) {
        this.code = code;
        this.name = name;
        this.abbrev = abbrev;
        this.description = description;
        this.sequenceType = sequenceType;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAbbrev() {
        return abbrev;
    }

    public void setAbbrev(String abbrev) {
        this.abbrev = abbrev;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SequenceType getSequenceType() {
        return sequenceType;
    }

    public void setSequenceType(SequenceType sequenceType) {
        this.sequenceType = sequenceType;
    }

}