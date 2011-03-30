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

package org.janelia.it.jacs.compute.service.search;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 12, 2007
 * Time: 1:01:36 PM
 *
 */
public class JacsAccessionSearchResultBuilderTest extends AbstractAccessionSearchResultBuilderTest {

    public JacsAccessionSearchResultBuilderTest(String name) {
        super(name);
    }

    protected AccessionSearchResultBuilder getCurrentAccessionResultBuilder() {
        return new JacsAccessionSearchResultBuilder();
    }

    public void testRetrieveExistingAccessionSearchResult() {
        testRetrieveExistingAccessionSearchResult("jcvi_read_299866");
        testRetrieveExistingAccessionSearchResult("JCVI_TGI_1096124280446");
        testRetrieveExistingAccessionSearchResult("cam_crcl_1");
        testRetrieveExistingAccessionSearchResult("cam_proj_gos");
        testRetrieveExistingAccessionSearchResult("jcvi_smpl_1103283000030");
    }

    public void testRetrieveValidNonExistingAccessionSearchResult() {
        testRetrieveValidNonExistingAccessionSearchResult("jcvi_read_-1");
    }

    public void testRetrieveInvalidAccessionSearchResult() {
        testRetrieveInvalidAccessionSearchResult("invalid_jcvi_read_-1");
    }


    private void testRetrieveExistingAccessionSearchResult(String testAcc) {
        try {
            AccessionSearchResultBuilder.AccessionSearchResult accResult =
                    retrieveAccessionSearchResult(testAcc);
            assertTrue(accResult != null);
            assertTrue(accResult.accession != null && accResult.accession.equalsIgnoreCase(testAcc));
        } catch(Exception e) {
            fail("testRetrieveExistingAccessionSearchResult " + testAcc);
        }
    }

    private void testRetrieveValidNonExistingAccessionSearchResult(String testAcc) {
        try {
            AccessionSearchResultBuilder.AccessionSearchResult accResult =
                    retrieveAccessionSearchResult(testAcc);
            assertTrue(accResult != null && accResult.accession == null);
        } catch(Exception e) {
            fail("testRetrieveValidNonExistingAccessionSearchResult " + testAcc);
        }
    }

    private void testRetrieveInvalidAccessionSearchResult(String testAcc) {
        try {
            AccessionSearchResultBuilder.AccessionSearchResult accResult =
                    retrieveAccessionSearchResult(testAcc);
            assertTrue(null==accResult || null==accResult.accession);
        } catch(Exception e) {
            fail("testRetrieveInvalidAccessionSearchResult " + testAcc);
        }
    }

}
