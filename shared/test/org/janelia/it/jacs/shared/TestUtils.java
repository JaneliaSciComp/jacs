
package org.janelia.it.jacs.shared;

import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.shared.blast.BlastGridMergeSort;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

/**
 * User: aresnick
 * Date: May 21, 2009
 * Time: 2:29:33 PM
 * <p/>
 * <p/>
 * Description:
 */
public class TestUtils
{
    private static final String SYSTEM_PATH_ROOT_PROPERTY = "shared.testfiles.root";
    private TestUtils() {}

    public static File getTestFile(String filename) {
        File testFile = new File(filename);
        if ( !testFile.exists() ) {
            String pathRoot = System.getProperty(SYSTEM_PATH_ROOT_PROPERTY);
            if ( pathRoot != null ) {
                File propRoot = new File(pathRoot);
                testFile = new File(propRoot,filename);
            }
        }
        return testFile;
    }

    public static File writeSerializedBlastTask(BlastTask blastTask, File outputDir) throws Exception
    {
        ObjectOutputStream oos = null;
        try
        {
            File serializedBlastTask = getSerializedBlastTaskFile(outputDir);
            FileOutputStream fos = new FileOutputStream(serializedBlastTask);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(blastTask);
            oos.flush();
            return serializedBlastTask;
        }
        finally
        {
            try
            {
                if ( oos != null ) oos.close();
            }
            catch (Exception e)
            {
                //
            }
        }
    }

    public static File getSerializedBlastTaskFile(File targetDirectory)
    {
        return new File(targetDirectory.getAbsolutePath(), BlastResultFileNode.PARSED_BLAST_RESULTS_COLLECTION_BASENAME + ".blastTask");
    }

    public static void writeSerializedBlastResultsFile(File blastResultFile, File outputDir) throws Exception
    {
        BlastGridMergeSort bgms = new BlastGridMergeSort(blastResultFile.getAbsolutePath());
        bgms.translateSingleOutputToObjects();
        // move oos file to output directory
        File oosFile = new File(blastResultFile.getAbsolutePath()+ BlastResultFileNode.DEFAULT_SERIALIZED_PBRC_FILE_EXTENSION);
        File newFile = new File(outputDir.getAbsolutePath(), BlastResultFileNode.PARSED_BLAST_RESULTS_COLLECTION_FILENAME);
        oosFile.renameTo(newFile);
    }
}
