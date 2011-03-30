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
public class SequenceType implements Serializable, IsSerializable {

    public static final int SEQTYPE_CODE_UNKNOWN = 0;
    public static final int SEQTYPE_CODE_AMINO_ACID = 1;
    public static final int SEQTYPE_CODE_NUCLEIC_ACID = 2;

    public static String NUCLEOTIDE = "nucleotide";
    public static String PEPTIDE = "peptide";
    public static String NOT_SPECIFIED = "not specified";

    public static String AMINO_ACIDS = "ARNDBCEQZGHILKMFPSTWYV*";
    public static String NUCLEIC_ACIDS = "ACGTUMRWSYKVHDBXN-";

    private static String NA_COMPLEMENTS = "TGCAAKYWSRMBDHVXN-";

    public static final SequenceType NA = new SequenceType(SEQTYPE_CODE_NUCLEIC_ACID, "NA", "Nucleic Acid", NUCLEOTIDE, NUCLEIC_ACIDS, NA_COMPLEMENTS);
    public static final SequenceType AA = new SequenceType(SEQTYPE_CODE_AMINO_ACID, "AA", "Amino Acid", PEPTIDE, AMINO_ACIDS, "");
    public static final SequenceType UNKNOWN = new SequenceType(SEQTYPE_CODE_UNKNOWN, "UNKNOWN", "Unknown", "", "", "");

    private int code;
    private String name;
    private String description;
    private String residueType;
    private String elements;
    private String complements;

    public SequenceType() {
    }

    public SequenceType(int code, String name, String description, String residueType, String elements, String complements) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.residueType = residueType;
        this.elements = elements;
        this.complements = complements;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getComplements() {
        return complements;
    }

    public void setComplements(String complements) {
        this.complements = complements;
    }

    public String getResidueType() {
        return residueType;
    }

    public void setResidueType(String residueType) {
        this.residueType = residueType;
    }

    public String getElements() {
        return elements;
    }

    public void setElements(String elements) {
        this.elements = elements;
    }
}