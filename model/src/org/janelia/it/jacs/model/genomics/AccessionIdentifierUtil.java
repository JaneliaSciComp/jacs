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
 * This class returns the type of accession
 * <p/>
 * TN: The original AccessionIdentifierUtil class created by Leonid used java.util.regex
 * which has not been emulated by GWT.  We had to fall back to non-regex string matching.
 *
 * @author Leonid Kagan
 * @author Tareq Nabeel
 */
public class AccessionIdentifierUtil {

    public static class AccessionType {
        private int type;
        private String description;

        private AccessionType(int type, String description) {
            this.type = type;
            this.description = description;
        }

        public int getType() {
            return type;
        }

        public String getDescription() {
            return description;
        }

    }

    public static final int INVALID_ACC = 0;

    // project accession types
    public static final int CAMERA_PROJECT_ACC = 100;
    public static final int CAMERA_PUBLICATION_ACC = 101;

    // genomic accession types
    public static final int CAMERA_READ_ACC = 200;
    public static final int CAMERA_ORF_ACC = 201;
    public static final int CAMERA_PROTEIN_ACC = 202;
    public static final int CAMERA_NCRNA_ACC = 203;
    public static final int CAMERA_SCAFFOLD_ACC = 210;

    public static final int NCBI_NT_ACC = 280;
    public static final int NCBI_AA_ACC = 281;
    public static final int NCBI_CNTG_ACC = 282;
    public static final int NCBI_GENF_ACC = 283;
    public static final int MISC_SEQ_ACC = 299;

    // metagenomic accession types
    public static final int CAMERA_BIOSAMPLE_ACC = 300;
    public static final int CAMERA_BIOMATERIAL_ACC = 301;

    // cluster accession types
    public static final int CAMERA_PROTEIN_CLUSTER_ACC = 400;


    private static final String SUPPORTED_ACCESSIONS = createSupportedAccessions();

    static public int getAccType(String acc) {
        AccessionType accType = getAccTypeWithDescription(acc);
        return accType.getType();
    }

    static public AccessionType getAccTypeWithDescription(String acc) {
        if (acc == null) {
            return new AccessionType(INVALID_ACC, "INVALID");
        }
        else if (acc.startsWith("CAM_PROJ_")) {
            return new AccessionType(CAMERA_PROJECT_ACC, "PROJECT");
        }
        else if (acc.startsWith("CAM_PUB_")) {
            return new AccessionType(CAMERA_PUBLICATION_ACC, "PUBLICATION");
        }
        else if (acc.indexOf("_SMPL_") > 0) {
            return new AccessionType(CAMERA_BIOSAMPLE_ACC, "BIO SAMPLE");
        }
        else if (acc.startsWith("CAM_CL_") ||
                acc.startsWith("CAM_CRCL_")) {
            return new AccessionType(CAMERA_PROTEIN_CLUSTER_ACC, "PROTEIN CLUSTER");
        }
        else if (acc.indexOf("_READ_") > 0) {
            return new AccessionType(CAMERA_READ_ACC, "READ");
        }
        else if (acc.indexOf("_ORF_") > 0) {
            return new AccessionType(CAMERA_ORF_ACC, "ORF");
        }
        else if (acc.startsWith("UNIPROT_NT_")) {
            return new AccessionType(CAMERA_ORF_ACC, "ORF");
        }
        else if (acc.indexOf("_PEP_") > 0) {
            return new AccessionType(CAMERA_PROTEIN_ACC, "PROTEIN");
        }
        else if (acc.startsWith("UNIPROT_SP_")) {
            return new AccessionType(CAMERA_PROTEIN_ACC, "PROTEIN");
        }
        else if (acc.indexOf("_NCRNA_") > 0) {
            return new AccessionType(CAMERA_NCRNA_ACC, "NCRNA");
        }
        else if (acc.indexOf("_SCAF_") > 0) {
            return new AccessionType(CAMERA_SCAFFOLD_ACC, "SCAFFOLD");
        }
        else if (acc.indexOf("_CNTG_") > 0) {
            return new AccessionType(NCBI_CNTG_ACC, "NCBI CONTIG");
        }
        else if (acc.indexOf("_GENF_") > 0) {
            return new AccessionType(NCBI_GENF_ACC, "NCBI GENE");
        }
        else if (acc.indexOf("_16S_") > 0) {
            return new AccessionType(CAMERA_NCRNA_ACC, "NCRNA");
        }
        else if (acc.startsWith("JCVI_NT_")) {
            return new AccessionType(CAMERA_NCRNA_ACC, "NCRNA");
        }
        else if (acc.indexOf("_NT_") > 0 ||
                acc.indexOf("_TGI_") > 0 ||
                acc.indexOf("_TRAN_") > 0) {
            return new AccessionType(MISC_SEQ_ACC, "NUCLEOTIDE SEQUENCE");
        }
        else {
            return new AccessionType(INVALID_ACC, "INVALID");
        }
    }

    public static boolean isProjectOrPublication(String acc) {
        int accType = AccessionIdentifierUtil.getAccType(acc);
        return accType == CAMERA_PROJECT_ACC || accType == CAMERA_PUBLICATION_ACC;
    }

    public static boolean isORF(String acc) {
        int accType = AccessionIdentifierUtil.getAccType(acc);
        return accType == CAMERA_ORF_ACC;
    }

    public static boolean isPeptide(String acc) {
        int accType = AccessionIdentifierUtil.getAccType(acc);
        return accType == CAMERA_PROTEIN_ACC;
    }

    public static boolean isNcRNA(String acc) {
        int accType = AccessionIdentifierUtil.getAccType(acc);
        return accType == CAMERA_NCRNA_ACC;
    }

    public static boolean isScaffold(String acc) {
        int accType = AccessionIdentifierUtil.getAccType(acc);
        return accType == CAMERA_SCAFFOLD_ACC;
    }

    public static boolean isRead(String acc) {
        int accType = AccessionIdentifierUtil.getAccType(acc);
        return accType == CAMERA_READ_ACC;
    }

    public static String getSupportedAccessions() {
        return SUPPORTED_ACCESSIONS;
    }

    private static String createSupportedAccessions() {
        StringBuffer buff = new StringBuffer();
        buff.append("CAM_PROJ_");
        buff.append(", ");
        buff.append("CAM_PUB_");
        buff.append(", ");
        buff.append("CAM_CRCL_");
        buff.append(", ");
        buff.append("CAM_CL_");
        buff.append(", ");
        buff.append("xxxx_SMPL_");
        buff.append(", ");
        buff.append("xxxx_READ_");
        buff.append(", ");
        buff.append("xxxx_ORF_");
        buff.append(", ");
        buff.append("xxxx_PEP_");
        buff.append(", ");
        buff.append("xxxx_SCAF_");
        buff.append(", ");
        buff.append("xxxx_NCRNA_");
        buff.append(", ");
        buff.append("xxxx_16S_");
        buff.append(", ");
        buff.append("xxxx_NT_");
        buff.append(", ");
        buff.append("xxxx_TGI_");
        buff.append(", ");
        buff.append("xxxx_CNTG_");
        buff.append(", and ");
        buff.append("xxxx_GENF_");
        return buff.toString();
    }

}
