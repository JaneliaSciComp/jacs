
package org.janelia.it.jacs.compute.service.export;

import org.janelia.it.jacs.compute.service.export.writers.ExportFastaWriter;
import org.janelia.it.jacs.model.tasks.export.FileNodeExportTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;
import org.janelia.it.jacs.shared.node.FastaUtil;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jul 11, 2008
 * Time: 2:49:38 PM
 *
 */
public class ExportTestFileNode extends ExportTestBase {

    public ExportTestFileNode() {
        super();
    }

    public ExportTestFileNode(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testFastaFileNodeExport() throws Exception {
        String fastaFileNodeId=createTestFastaFileNode(TEST_USER_NAME);
        ArrayList<String> accessionList=new ArrayList<String>();
        FileNodeExportTask task = new FileNodeExportTask(
                fastaFileNodeId,
                ExportWriterConstants.EXPORT_TYPE_CURRENT,
                accessionList,
                null // SortArgument list 
        );
        task.setOwner(TEST_USER_NAME);    
        submitJobAndWaitForCompletion("FileExport", task);
    }

    public String createTestFastaFileNode(String userName) throws Exception {
        String defline=">ThisIsATestDefline /key1=value1 /key2=value2 /key3=value3";
        String sequence="ACATGTCGATGCATGCTAGCTAGCTAGCTGACTAGTCGATCGTAGCTACGATCGTGTCTCTAGAGGCTCAGACTACGCTATGATCGTAGC"+
                        "TATATCGCTGCTGTAGTATATATATATGCGCCGCGTGTTGTGTGTGATCACCACACACACTCTCTCCTCTCTAGAGCGTGATCTACGATC";
        FastaFileNode testNode=new FastaFileNode();
        testNode.setLength(new Long(sequence.length()));
        testNode.setSequenceCount(1);
        testNode.setSequenceType(FastaFileNode.NUCLEOTIDE);
        testNode.setOwner(userName);
        testNode=(FastaFileNode)computeBean.createNode(testNode);
        System.out.println("testNode id="+testNode.getObjectId());
        File fastaFile=new File(testNode.getFilePathByTag(FastaFileNode.TAG_FASTA));
        File dir=fastaFile.getParentFile();
        System.out.println("Creating dir="+dir.getAbsolutePath());
        if (!dir.mkdirs()) {
            System.err.println("Could not create directory - check jacs.properties for filestore paths");
        } else {
            System.out.println("Success");
        }
        FileWriter fileWriter=new FileWriter(fastaFile);
        fileWriter.write(FastaUtil.formatFasta(defline, sequence, ExportFastaWriter.FASTA_SEQ_WIDTH));
        fileWriter.close();
        return testNode.getObjectId().toString();
    }

}
