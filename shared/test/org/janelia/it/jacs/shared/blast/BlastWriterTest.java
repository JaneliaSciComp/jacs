
package org.janelia.it.jacs.shared.blast;

import junit.framework.TestCase;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.shared.TestUtils;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;

/**
 * User: aresnick
 * Date: May 18, 2009
 * Time: 3:23:48 PM
 * <p/>
 * <p/>
 * Description:
 */
abstract public class BlastWriterTest extends TestCase {

    protected static Logger logger;

    static{
        Logger.getRootLogger().addAppender(new ConsoleAppender(new SimpleLayout()));
    }

    public BlastWriterTest() {
        super();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    protected void blastWriterTestImpl(BlastTask blastTask, String inputDataFilename) throws Exception {
        
        // generate ParsedBlastResultCollection
        String root="shared"+ File.separator+"testfiles"+File.separator+"BlastParser"+File.separator+"BLAST Outputs"+File.separator;
        File testFile = TestUtils.getTestFile(root+inputDataFilename);
        logger.info("Working on: "+testFile + "; test file existance: " +testFile.exists());

        // create an empty test directory for this test case
        File testDataOutDir = new File(testFile.getParentFile(),
                                       "test"+getOutputFormatKey()+"_"+inputDataFilename);
        FileUtil.deleteDirectory(testDataOutDir);
        if (! testDataOutDir.mkdirs())
            fail("Unable to create output directory");

        // Create default blast task in output dir for test case.
        try {
            TestUtils.writeSerializedBlastTask(blastTask, testDataOutDir);
        } catch (Exception e) {
            logger.error(e);
            fail("Unable to create blastTask in output directory" + e.getMessage());
        }

        // serialize test file and write it to target output diectory
        try
        {
            TestUtils.writeSerializedBlastResultsFile(testFile, testDataOutDir);
        }
        catch (Exception e)
        {
            logger.error(e);
            fail("Failed to serialize " + testFile.getName() + "  " +e.getMessage());
        }

        // sort and merge blast results items
        // used to get BlastResultCollectionConverter constructor expected query hit count
        BlastGridContinuousMergeSort bgcms=new BlastGridContinuousMergeSort(
                testDataOutDir.getAbsolutePath(), // serialized files directory
                1, // number of partitions
                5000); // number of top hits per query
        bgcms.mergeAndSortObjectResults(
                2, // seconds for file to be finished
                172800, // seconds for new file to appear - 172800 = 2 days
                null); // task is null for command-line case

        // persist results as desired output format
        BlastResultCollectionConverter brcc =
                new BlastResultCollectionConverter(testDataOutDir,
                                                   0,
                                                   bgcms.getQueryCountWithHits(),
                                                   true,
                                                   true,
                                                   new String[]{getOutputFormatKey()});
        try {
            brcc.process();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e);
            fail("Failed to write results XML output " +e.getMessage());
        }
    }

    abstract protected String getOutputFormatKey();
}
