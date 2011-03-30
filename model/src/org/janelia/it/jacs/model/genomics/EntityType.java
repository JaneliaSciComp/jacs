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
 * Date: Nov 6, 2006
 * Time: 3:17:48 PM
 */
public class EntityType implements Serializable, IsSerializable {

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


    public static final EntityType UNKNOWN = new EntityType(ENTITY_CODE_UNKNOWN, "Unknown", "UNK", "", SequenceType.UNKNOWN);
    public static final EntityType NUCLEOTIDE = new EntityType(ENTITY_CODE_NUCLEOTIDE, "Nucleotide", "NUC", "", SequenceType.NA);
    public static final EntityType CHROMOSOME = new EntityType(ENTITY_CODE_CHROMOSOME, "Chromosome", "CHR", "", SequenceType.NA);
    public static final EntityType SCAFFOLD = new EntityType(ENTITY_CODE_SCAFFOLD, "Scaffold", "SCAF", "", SequenceType.NA);
    public static final EntityType READ = new EntityType(ENTITY_CODE_READ, "Read", "READ", "", SequenceType.NA);
    public static final EntityType GENE = new EntityType(ENTITY_CODE_GENE, "Gene", "Gene", "", SequenceType.NA);
    public static final EntityType ORF = new EntityType(ENTITY_CODE_ORF, "ORF", "ORF", "", SequenceType.NA);
    public static final EntityType PROTEIN = new EntityType(ENTITY_CODE_PROTEIN, "Protein", "PRO", "", SequenceType.AA);
    public static final EntityType PEPTIDE = new EntityType(ENTITY_CODE_PEPTIDE, "Peptide", "PEP", "", SequenceType.AA);
    public static final EntityType NON_CODING_RNA = new EntityType(ENTITY_CODE_NON_CODING_RNA, "ncRNA", "ncRNA", "", SequenceType.NA);


    private int code;
    private String name;
    private String abbrev;
    private String description;
    private SequenceType sequenceType;

    public EntityType() {
    }

    public EntityType(int code, String name, String abbrev, String description, SequenceType sequenceType) {
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