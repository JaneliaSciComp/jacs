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

package org.janelia.it.jacs.shared.blast;

import junit.framework.TestCase;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.janelia.it.jacs.model.tasks.blast.BlastNTask;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.shared.TestUtils;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Feb 7, 2007
 * Time: 4:29:38 PM
 *
 */
public class BlastGridMergeSortTest extends TestCase {

    static Logger logger = Logger.getLogger(BlastGridMergeSortTest.class.getName());
    static{
        Logger.getRootLogger().addAppender(new ConsoleAppender(new SimpleLayout()));
    }

    private final static String bgmsRoot="shared"+ File.separator+"testfiles"+File.separator+"BlastGridMergeSort"+File.separator;
    private final File testDataDir = TestUtils.getTestFile(bgmsRoot);
    private final File testDataOutDir = TestUtils.getTestFile(bgmsRoot + "output");


    public void setUp() throws Exception {
        // clean up
        FileUtil.deleteDirectory(testDataOutDir);
        if (! testDataOutDir.mkdirs())
            fail("Unable to create output directory");

        /*
           Create default blast task in output dir for test case from default object.
           Used because lastTask/BlastNTask class changes cause serialized object to become unusable
        */
        try {
            TestUtils.writeSerializedBlastTask(new BlastNTask(), testDataOutDir);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unable to create blastTask in output directory" + e.getMessage());
        }

        /*
           copy existing serialized blastTask to output directory
           despite serialization risks, may be necessary if/when xml ouptut validation is performed
           because the serialized object would guarantee identical blast task paramater values
        File btFile = TestUtils.getSerializedBlastTaskFile(testDataDir);
        try{
            FileUtil.copyFile(btFile,
                              TestUtils.getSerializedBlastTaskFile(testDataOutDir));
        }
        catch (IOException e)
        {
            fail("Unable to copy blastTask to output directory" + e.getMessage());            
        }
        */

        super.setUp();
    }

    public void tearDown() throws Exception {

        //FileUtil.deleteDirectory(testDataOutDir);
        super.tearDown();
    }

    public void xxxtestDeterminationOfSecondPass(){}

    public void xxxtestConversion() {
        try {
            BlastGridMergeSort bgms = new BlastGridMergeSort(bgmsRoot+"blast.out.r_0");
            bgms.translateSingleOutputToObjects();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void xxxtestBlastMergeSort() {
        executeMergeAndSort(0,85, 1000);
    }

    public void testBlastGridContinuousMergeSort() {
        // first serialize XML output for each file in the directory
        File[] xmlFileArr = testDataDir.listFiles(new ExtensionFilenameFilter(".xml"));

        for (File blastResultFile: xmlFileArr)
        {
            try
            {
                BlastGridMergeSort bgms = new BlastGridMergeSort(blastResultFile.getAbsolutePath());
                bgms.translateSingleOutputToObjects();
                // move oos file to output directory
                File oosFile = new File(blastResultFile.getAbsolutePath()+BlastResultFileNode.DEFAULT_SERIALIZED_PBRC_FILE_EXTENSION);
                File newFile = new File(testDataOutDir.getAbsolutePath(), oosFile.getName());
                oosFile.renameTo(newFile);
            }
            catch (Exception e)
            {
                logger.error(e);
                fail("Failed to serialize " + blastResultFile.getName() + "  " +e.getMessage());
            }
        }


        // merge all
        BlastGridContinuousMergeSort bgms=new BlastGridContinuousMergeSort(
                testDataOutDir.getAbsolutePath(), // serialized files directory
                xmlFileArr.length, // number of partitions
                6); // number of top hits per query
        ParsedBlastResultCollection parsedBlastResultCollection = null;
        try
        {
            parsedBlastResultCollection = bgms.mergeAndSortObjectResults(
                2, // seconds for file to be finished
                172800, // seconds for new file to appear - 172800 = 2 days
                null); // task is null for command-line case
        }
        catch (IOException e)
        {
            logger.error(e);
            fail("Failed to merge due to IO error " +e.getMessage());
        }
        catch (ClassNotFoundException e)
        {
            logger.error(e);
            fail("Failed to merge due to ClassNotFoundException error " +e.getMessage());
        }

        try
        {
            bgms.writeParsedBlastResultCollectionToFs(parsedBlastResultCollection, testDataOutDir, bgms.getQueryCountWithHits());
        }
        catch (IOException e)
        {
            logger.error(e);
            fail("Failed to write results count to disk due to IO error " +e.getMessage());
        }

        try
        {
            String totalHitCountFilePath=testDataOutDir.getAbsolutePath() + File.separator + "totalHits.out";
            BlastSharedUtil.writeTotalBlastHits(parsedBlastResultCollection, totalHitCountFilePath);
        }
        catch (IOException e)
        {
            logger.error(e);
            fail("Failed to write merge result to disk due to IO error " +e.getMessage());
        }

        //  dump resuls as a text file
        try
        {
            ParsedBlastResult lastPbr = null;
            PrintWriter textOut = new PrintWriter(testDataOutDir.getAbsolutePath() + File.separator + BlastResultFileNode.PARSED_BLAST_RESULTS_COLLECTION_BASENAME + ".txt");
            for (ParsedBlastResult pbr: parsedBlastResultCollection.getParsedBlastResults())
            {
                if (lastPbr == null || ! lastPbr.getQueryId().equals(pbr.getQueryId()))
                {
                    textOut.println("QUERY ID: " + pbr.getQueryId());
                    lastPbr = pbr;
                }
                textOut.println("    " + pbr.toFormattedString());
            }
            textOut.close();
        }
        catch (IOException e)
        {
            logger.error(e);
            fail("Failed to write results text output " +e.getMessage());
        }
        // persist results as XML
        BlastResultCollectionConverter brcc =
                new BlastResultCollectionConverter(testDataOutDir, 0, bgms.getQueryCountWithHits(), true, true, new String[]{"xml","btab"});
//        BlastObjectToXMLConverter boc=new BlastObjectToXMLConverter(
//                testDataOutDir, BigInteger.ZERO, BigInteger.valueOf(bgms.getQueryCountWithHits()), true, true);
        try {
            brcc.process();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e);
            fail("Failed to write results XML output " +e.getMessage());
        }

        // TODO: verify validity of the final XML file
        
    }

    public void xxxtestLoopedMergeSort() {
        int lastPartition = 107;//85;
        int chunkSize = 0;  //Chunk size of 0 means the files are parsed individually
        for (int i = 0; i <= lastPartition; i+=(chunkSize+1)) {
            int endPartition = i+chunkSize;
            // If the chunkSize goes past the last partition then set end to that
            if (endPartition>lastPartition) {
                endPartition = lastPartition;
            }
            executeMergeAndSort(i, endPartition, 1000);
        }
    }


    public void xxx_testReadOfListObjects() {
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try{
            BlastGridMergeSort bgms = new BlastGridMergeSort(bgmsRoot, 0, 0, 100);
            ParsedBlastResultCollection pbrc = bgms.mergeAndSortObjectResults(5,null);
        }
        catch (FileNotFoundException e) {
            logger.error(e);
            e.printStackTrace();
        }
        catch (IOException e) {
            logger.error(e);
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            logger.error(e);
            e.printStackTrace();
        }
        catch (Exception e) {
            logger.error(e);
            e.printStackTrace();
        } finally{
            try {
                if (null!=ois) {
                    ois.close();
                }
                if (null!=fis) {
                    fis.close();
                }
            }
            catch (IOException e) {
                logger.error(e);
                e.printStackTrace();
            }
        }
    }


    // Brute force method - does not scale
    public void xxxtestBriteForceReadOfListObjects() {
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        ArrayList<ParsedBlastResult> results = new ArrayList<ParsedBlastResult>();

        try {
            File[] objectFileList = testDataDir.listFiles(new BlastGridMergeSort.MyFilenameFilter());

            for (File tmpFile: objectFileList) {
                fis = new FileInputStream(tmpFile);
                ois = new ObjectInputStream(fis);
                logger.info("Reading file: "+tmpFile.getName());
                results.addAll((ArrayList<ParsedBlastResult>)ois.readObject());
            }
            logger.info("There are "+results.size()+" total results.");
            Collections.sort(results);
            //outputToFile(results);
            logger.info("Sorted");
            ArrayList<ParsedBlastResult> finalResults = new ArrayList<ParsedBlastResult>(results.subList(0, 50000));
            logger.info("Last collection has "+finalResults.size()+" items.");
        }
        catch (FileNotFoundException e) {
            logger.error(e);
            e.printStackTrace();
        }
        catch (IOException e) {
            logger.error(e);
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            logger.error(e);
            e.printStackTrace();
        }
        finally{
            try {
                if (null!=ois) {
                    ois.close();
                }
                if (null!=fis) {
                    fis.close();
                }
            }
            catch (IOException e) {
                logger.error(e);
                e.printStackTrace();
            }
        }
    }

    private void outputToFile(ArrayList newItems) {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            for (int i=0; i < 180; i++) {
                fos = new FileOutputStream(bgmsRoot+File.separator+"TESTparsedBlastResult."+i+ BlastResultFileNode.DEFAULT_SERIALIZED_PBRC_FILE_EXTENSION);
                oos = new ObjectOutputStream(fos);
                oos.writeObject(newItems);
                oos.close();
            }
        }
        catch (IOException e) {
            logger.error(e);
            e.printStackTrace();
        }
        finally{
            try {
                if (null!=oos) {
                    oos.flush();
                    oos.close();
                }
                if (null!=fos) {
                    fos.flush();
                    fos.close();
                }
            }
            catch (IOException e) {
                logger.error(e);
                e.printStackTrace();
            }
        }
    }

    private void executeMergeAndSort(int startPartition, int lastPartitionProcessed, int numberOfTopHitsSaved){
        logger.debug("Working on: Merge");
        BlastGridMergeSort bgms = new BlastGridMergeSort(bgmsRoot, startPartition, lastPartitionProcessed, numberOfTopHitsSaved);
        try {
            bgms.mergeAndSortObjectResults(10,null);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  
        }
        logger.debug("\nDone.");
    }

    /**
      * Filter to look for serialized ParsedBlastResultCollection objects.  The latter objects contain blast hits and
      * any relevant deflines.
      */
     public static class ExtensionFilenameFilter implements FilenameFilter {
         private String ext = "";
         public ExtensionFilenameFilter(String extension)
         {
             if (extension != null)
             {
                if (extension.startsWith("."))
                    ext = extension;
                 else
                    ext = "." + extension;
             }
         }
         public boolean accept(File dir, String name) {
             return null != name && name.endsWith(ext);
         }
     }

}
