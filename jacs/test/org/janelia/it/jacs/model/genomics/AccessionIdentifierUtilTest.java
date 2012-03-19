
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
        assertEquals(AccessionIdentifierUtil.getAccType("CAM_PROJ_ASDFDR"),AccessionIdentifierUtil.PROJECT_ACC);
        assertEquals(AccessionIdentifierUtil.getAccType("CAM_PUB_SDFER"),AccessionIdentifierUtil.PUBLICATION_ACC);
        assertEquals(AccessionIdentifierUtil.getAccType("JCVI_READ_123344523"),AccessionIdentifierUtil.READ_ACC);
        assertEquals(AccessionIdentifierUtil.getAccType("SCUMS_READ_123344523"),AccessionIdentifierUtil.READ_ACC);
        assertEquals(AccessionIdentifierUtil.getAccType("JCVI_ORF_123344523"),AccessionIdentifierUtil.ORF_ACC);
        assertEquals(AccessionIdentifierUtil.getAccType("JCVI_PEP_23452342"),AccessionIdentifierUtil.PROTEIN_ACC);
        assertEquals(AccessionIdentifierUtil.getAccType("JCVI_SCAF_345623456"),AccessionIdentifierUtil.SCAFFOLD_ACC);
        assertEquals(AccessionIdentifierUtil.getAccType("NCBI_NT_3456345"),AccessionIdentifierUtil.NCBI_NT_ACC);
        assertEquals(AccessionIdentifierUtil.getAccType("NCBI_PEP_3456345"),AccessionIdentifierUtil.NCBI_AA_ACC);
        assertEquals(AccessionIdentifierUtil.getAccType("NCBI_CNTG_3456345"),AccessionIdentifierUtil.NCBI_CNTG_ACC);
    }

}
