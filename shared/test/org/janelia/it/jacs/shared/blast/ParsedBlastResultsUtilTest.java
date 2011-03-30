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

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Feb 22, 2008
 * Time: 8:58:43 AM
 *
 */
public class ParsedBlastResultsUtilTest extends TestCase {

    static Logger logger = Logger.getLogger(BlastGridMergeSortTest.class.getName());
    static {
        Logger.getRootLogger().addAppender(new ConsoleAppender(new SimpleLayout()));
    }

    public ParsedBlastResultsUtilTest() {
        super();
    }

    public void testSaveParsedBlastResults() {
//        ParsedBlastResultsUtil pbrTool = new ParsedBlastResultsUtil(-1);
//        try {
//            File testBlastN = new File(getFullTestFileName("blastn.xml"));
//            File blastnParsedResFile = pbrTool.saveParsedBlastResults(testBlastN);
//            assertTrue(blastnParsedResFile.exists() && blastnParsedResFile.length() > 0);
//            // check the output
//            ParsedBlastResultCollection pbrSet = pbrTool.parseBlastResults(testBlastN,true);
//            checkSavedParsedBlastResults(pbrSet,blastnParsedResFile);
//            // clean up after yourself
//            blastnParsedResFile.deleteOnExit();
//        } catch(Exception e) {
//            logger.error(e);
//            fail(e.toString());
//        }
    }

    public void testMergeIdenticalBlastResultCollections() {
//        ParsedBlastResultsUtil pbrTool = new ParsedBlastResultsUtil(-1);
//        try {
//            File testBlastN = new File(getFullTestFileName("blastn.xml"));
//            ParsedBlastResultCollection pbrSet = pbrTool.parseBlastResults(testBlastN,false);
//            Collections.sort(pbrSet.getParsedBlastResults());
//            File blastnParsedResFile = new File(getFullTestFileName("blastn.oos"));
//            pbrTool.writeParsedBlastResults(pbrSet,false,new FileOutputStream(blastnParsedResFile));
//            File mergedBlastResFile = new File(getFullTestFileName("mergedblastn.oos"));
//            pbrTool.mergeBlastResultCollections(blastnParsedResFile,blastnParsedResFile,mergedBlastResFile);
//            ArrayList<ParsedBlastResult> pbrList = new ArrayList<ParsedBlastResult>(pbrSet.getParsedBlastResults());
//            pbrList.addAll(pbrSet.getParsedBlastResults());
//            Collections.sort(pbrList);
//            ParsedBlastResultCollection mergedPbrSet = new ParsedBlastResultCollection();
//            mergedPbrSet.setPbrList(pbrList);
//            checkSavedParsedBlastResults(mergedPbrSet,mergedBlastResFile);
//            // clean up after yourself
//            blastnParsedResFile.deleteOnExit();
//            mergedBlastResFile.deleteOnExit();
//        } catch(Exception e) {
//            logger.error(e);
//            fail(e.toString());
//        }
    }

    public void testMergeDifferentBlastResultCollections() {
//        ParsedBlastResultsUtil pbrTool = new ParsedBlastResultsUtil(-1);
//        try {
//            File testF1 = new File(getFullTestFileName("MergeTestFiles/f1.xml"));
//            File f1PBC = pbrTool.saveParsedBlastResults(testF1);
//            assertTrue(f1PBC.exists());
//            File testF2 = new File(getFullTestFileName("MergeTestFiles/f2.xml"));
//            File f2PBC = pbrTool.saveParsedBlastResults(testF2);
//            assertTrue(f2PBC.exists());
//
//            File mergedBlastResFile = new File(getFullTestFileName("MergeTestFiles/merged_f1_f2.oos"));
//            pbrTool.mergeBlastResultCollections(f1PBC,f2PBC,mergedBlastResFile);
//            assertTrue(mergedBlastResFile.exists() &&
//                    mergedBlastResFile.length() >= f1PBC.length() && mergedBlastResFile.length() >= f2PBC.length());
//
//            // clean up after yourself
//            f1PBC.deleteOnExit();
//            f2PBC.deleteOnExit();
//            mergedBlastResFile.deleteOnExit();
//        } catch(Exception e) {
//            logger.error(e);
//            fail(e.toString());
//        }
    }

    private void checkSavedParsedBlastResults(ParsedBlastResultCollection pbrSet,File blastnParsedResFile)
            throws Exception {
        // check the output
        ObjectInputStream resStream = null;
        int pbrSetSize = pbrSet.size();
        int nSavedResults = 0;
        try {
            resStream = new ObjectInputStream(new FileInputStream(blastnParsedResFile));
            Iterator<ParsedBlastResult> pbrSetItr = pbrSet.iterator();
            for(;;nSavedResults++) {
                ParsedBlastResult pbrFromStream = null;
                pbrFromStream = (ParsedBlastResult)resStream.readObject();
                if(pbrFromStream == null) {
                    assertTrue(!pbrSetItr.hasNext());
                    break;
                }
                assertTrue(pbrSetItr.hasNext());
                ParsedBlastResult pbrFromCollection = pbrSetItr.next();
                assertTrue(pbrFromStream.compareTo(pbrFromCollection) == 0);
            }
        } catch(Exception checkExc) {
            if(resStream != null) {
                try {
                    resStream.close();
                } catch(Exception ignore) {
                }
            }
            logger.error(checkExc);
            fail(checkExc.toString());
        }
        assertTrue(pbrSetSize == nSavedResults);
    }

    private String getFullTestFileName(String fname) {
        String testDirectory="C:\\dev\\CameraWS\\shared"+ File.separator+"testfiles"+File.separator+"BlastParser"+File.separator+"BLAST Outputs"+File.separator;
        return testDirectory + fname;
    }

}
