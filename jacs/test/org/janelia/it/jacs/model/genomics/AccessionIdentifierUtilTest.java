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

import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Jan 10, 2007
 * Time: 2:14:32 PM
 */
public class AccessionIdentifierUtilTest extends TestCase {  
    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testMatchingAccs() throws Exception {
        assertEquals(AccessionIdentifierUtil.getAccType("CAM_PROJ_ASDFDR"),AccessionIdentifierUtil.CAMERA_PROJECT_ACC);
        assertEquals(AccessionIdentifierUtil.getAccType("CAM_PUB_SDFER"),AccessionIdentifierUtil.CAMERA_PUBLICATION_ACC);
        assertEquals(AccessionIdentifierUtil.getAccType("JCVI_READ_123344523"),AccessionIdentifierUtil.CAMERA_READ_ACC);
        assertEquals(AccessionIdentifierUtil.getAccType("SCUMS_READ_123344523"),AccessionIdentifierUtil.CAMERA_READ_ACC);
        assertEquals(AccessionIdentifierUtil.getAccType("JCVI_ORF_123344523"),AccessionIdentifierUtil.CAMERA_ORF_ACC);
        assertEquals(AccessionIdentifierUtil.getAccType("JCVI_PEP_23452342"),AccessionIdentifierUtil.CAMERA_PROTEIN_ACC);
        assertEquals(AccessionIdentifierUtil.getAccType("JCVI_SCAF_345623456"),AccessionIdentifierUtil.CAMERA_SCAFFOLD_ACC);
        assertEquals(AccessionIdentifierUtil.getAccType("NCBI_NT_3456345"),AccessionIdentifierUtil.NCBI_NT_ACC);
        assertEquals(AccessionIdentifierUtil.getAccType("NCBI_PEP_3456345"),AccessionIdentifierUtil.NCBI_AA_ACC);
        assertEquals(AccessionIdentifierUtil.getAccType("NCBI_CNTG_3456345"),AccessionIdentifierUtil.NCBI_CNTG_ACC);
    }

}
