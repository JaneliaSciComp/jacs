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

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.shared.blast.blastxmlparser.BlastXMLParser;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Feb 7, 2007
 * Time: 12:48:25 PM
 */
public class BlastGridMergeSort {

    public static transient final String BLAST_HIT_COUNT_SUFFIX = ".totalHitCount";
    public static transient final String QUERY_HIT_COUNT_SUFFIX = ".queryCountWithHits";

    private File blastResultDir, blastResultFile;
    private int firstPartitionProcessed, lastPartitionProcessed, numberOfTopHitsPerQuerySaved;

    public BlastGridMergeSort(String blastResultFile) {
        this.blastResultFile = new File(blastResultFile);
    }

    public BlastGridMergeSort(String blastResultDir, int firstPartitionProcessed, int lastPartitionProcessed, int numberOfTopHitsPerQuerySaved) {
        this.blastResultDir = new File(blastResultDir);
        this.firstPartitionProcessed = firstPartitionProcessed;
        this.lastPartitionProcessed = lastPartitionProcessed;
        this.numberOfTopHitsPerQuerySaved = numberOfTopHitsPerQuerySaved;
    }

    /**
     * This method assumes the BlastGridMergeSort object was constructed with the blastResultFile constructor.
     * It assumes that the blast -b option has already limited the size of results.
     *
     * @throws Exception - exception why it couldn't translate the objects
     */
    public ParsedBlastResultCollection translateSingleOutputToObjects() throws Exception {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        boolean parserSuccess = false;
        File oosFile = new File(blastResultFile.getAbsolutePath() + BlastResultFileNode.DEFAULT_SERIALIZED_PBRC_FILE_EXTENSION);
        try {
            BlastXMLParser bxp = new BlastXMLParser();
            fos = new FileOutputStream(oosFile.getAbsolutePath());
            oos = new ObjectOutputStream(fos);

            // Check for the result directory
            if (!blastResultFile.exists()) {
                System.out.println("The directory " + blastResultDir.getAbsolutePath() + " does not exist.");
                throw new FileNotFoundException("Could not find the file " + blastResultFile.getAbsolutePath());
            }

            // Now loop through the files requested, and grab the contents
            // Read the file and get the objects out of it.

            // Simulate error for debugging
//            if (Math.random()<0.10) {
//                System.out.println("Intentionally throwing test Exception before loading blast parser");
//                throw new Exception("This is a simulated exception");
//            } else {
//                System.out.println("This instance running blast parser normally");
//            }

            bxp.loadXML(blastResultFile);
            bxp.transferToParsedBlastResultSet();

            // Order the BlastHits
            ParsedBlastResultCollection parsedBlastResultCollection = bxp.getParsedBlastResultCollection();
            if (null != parsedBlastResultCollection) {
                // Write the object to the disk for later retrieval
                // NOTE: We could write out another object if it would save a lot of time not looping.  A map of hits per query would work better.
                System.out.println("\nWriting " + parsedBlastResultCollection.getParsedBlastResults().size() + " ParsedBlastResults to file " + blastResultFile.getAbsolutePath() + BlastResultFileNode.DEFAULT_SERIALIZED_PBRC_FILE_EXTENSION);
                oos.writeObject(parsedBlastResultCollection);
                oos.close();
            }

            parserSuccess = true;

            return parsedBlastResultCollection;
        }
        catch (Exception ex) {

            ex.printStackTrace();

            if (!parserSuccess) {
                // First close streams
                try {
                    if (oos != null) {
                        oos.close();
                    }
                    if (fos != null) {
                        fos.close();
                    }
                }
                catch (Exception closeEx) {
                    // ignore
                }
                // Next, replace with 0-content file to ensure an incomplete file is not mistaken for success
                try {
                    oos = null;
                    fos = null;
                    boolean deleteSuccess = oosFile.delete();
                    if (!deleteSuccess){
                        System.err.println("Unable to delete oos file: "+oosFile.getAbsolutePath());
                    }
                }
                catch (Exception deleteEx) {
                    // ignore
                }
                oosFile.createNewFile();
            }

            throw new Exception("There was an error executing method translateSingleOutputToObjects. " + ex.getMessage());
        }
        finally {
            try {
                if (null != oos) {
                    oos.flush();
                    oos.close();
                }
                if (null != fos) {
                    fos.flush();
                    fos.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * This method is able to loop through xml blast output files and sort/truncate hits based on score and
     * other blast parameters.  I refer to ParsedBlastResults as "hits".
     *
     * @param maximumFileReadyWaitTimeInSeconds
     *         how long we wait for nfs to vouchesafe the file
     * @return ParsedBlastResultCollection which contains hits or all queries and deflines for all subjects and queries
     * @throws IOException            could not access one of the blast output files
     * @throws ClassNotFoundException could not read in the object stream. there should at least be an empty collection
     */
    public ParsedBlastResultCollection mergeAndSortObjectResults(int maximumFileReadyWaitTimeInSeconds, Task blastTask) throws IOException, ClassNotFoundException {
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        Map<String, ArrayList<ParsedBlastResult>> queryToHitsMap = new HashMap<String, ArrayList<ParsedBlastResult>>();
        Map<String, ParsedBlastResult> worstHitPerQueryMap = new HashMap<String, ParsedBlastResult>();
        Map<String, String> deflineMap = new HashMap<String, String>();
        ParsedBlastResultCollection referencePBRC = new ParsedBlastResultCollection();

        try {
            File[] objectFileList = blastResultDir.listFiles(new MyFilenameFilter());
            // We're assuming that all messages are error/warning because only
            // error/warning messages are being placed in task messages currently.
            // Should this change, we'll need to beef up task to parse the messages
            // and tell us if there are errors/warnings through it's interface
            if (blastTask != null && blastTask.getMessages().size() > 0) {
                if (objectFileList == null || objectFileList.length == 0) {
                    throw new RuntimeException("Blast error and no hits.  Task messages: " + blastTask.getMessages());
                }
            }
            if (null == objectFileList || 0 >= objectFileList.length) {
                System.out.println("There are no results to parse.");
                return new ParsedBlastResultCollection();
            }

            System.out.println("************* numberOfTopHitsPerQuerySaved =" + numberOfTopHitsPerQuerySaved);
            // Now, loop through the files and only keep the best...
            for (File file : objectFileList) {
                System.out.println("Reading file: " + file.getAbsolutePath());
                waitTillFileIsReady(file, maximumFileReadyWaitTimeInSeconds);
                fis = new FileInputStream(file);
                ois = new ObjectInputStream(fis);
                ParsedBlastResultCollection tmpPBRS = (ParsedBlastResultCollection) ois.readObject();

                // If there are hits in the file then organize them
                if (null != tmpPBRS && null != tmpPBRS.getParsedBlastResults() && tmpPBRS.getParsedBlastResults().size() > 0) {
                    // Loop through the hits
                    for (ParsedBlastResult parsedBlastResult : tmpPBRS.getParsedBlastResults()) {
                        // If the query map knows nothing about the queryId start a new list
                        if (!queryToHitsMap.keySet().contains(parsedBlastResult.queryId)) {
                            queryToHitsMap.put(parsedBlastResult.queryId, new ArrayList<ParsedBlastResult>());
                        }
                        // Now get that list and see if we need to add a new item to it
                        // They are already sorted by score after the blasts were run
                        ArrayList<ParsedBlastResult> queryList = queryToHitsMap.get(parsedBlastResult.queryId);
                        ParsedBlastResult tmpLowestScoreHitForQuery = worstHitPerQueryMap.get(parsedBlastResult.queryId);
                        boolean addHit = false;
                        // If the queryList has items then compare
                        if (queryList.size() > 0) {
                            int comparison = tmpLowestScoreHitForQuery.compareTo(parsedBlastResult);

                            // If the score of the item we are comparing to is greater than our tmpLowestScoreHitForQuery, AND
                            // the reference collection is >= the number of top hits the user wants saved, then we do not add
                            if (comparison < 0 && queryList.size() >= numberOfTopHitsPerQuerySaved) {
//                                System.out.println("The reference expect score of " + tmpLowestScoreHitForQuery.expectScore + " is better than " + parsedBlastResult.expectScore + " and the hits collection is full.");
                            }
                            else {
                                // If still in this loop, add the new hit
                                addHit = true;
                                // Update the worst list, if necessary
                                if (comparison < 0) {
                                    worstHitPerQueryMap.put(parsedBlastResult.queryId, parsedBlastResult);
                                }
                            }
                        }
                        // The list is empty so add the hit.  This also prevents a null exception on tmpLowestScoreHitForQuery
                        else {
                            addHit = true;
                            worstHitPerQueryMap.put(parsedBlastResult.queryId, parsedBlastResult);
                        }

                        if (addHit) {
                            // Add the hit to it's query list
                            queryToHitsMap.get(parsedBlastResult.queryId).add(parsedBlastResult);
                            // Maintain the deflines which go with the result just added.
                            deflineMap.put(parsedBlastResult.queryId, tmpPBRS.getDeflineMap().get(parsedBlastResult.queryId));
                            deflineMap.put(parsedBlastResult.subjectId, tmpPBRS.getDeflineMap().get(parsedBlastResult.subjectId));
                        }
                    }
                }

                // When done, iterate through the query lists and sort, truncate, and put back each one before going to the next file
                // NOTE: This temporarily creates a bloated defline map as we are truncating hits which could orphan deflines.  We clear this later.
                // We are burning cycles in order to keep the memory footprint down.
                for (String tmpQueryId : queryToHitsMap.keySet()) {
                    ArrayList<ParsedBlastResult> tmpResultsList = queryToHitsMap.get(tmpQueryId);
                    Collections.sort(tmpResultsList);
                    int tmpSize = (tmpResultsList.size() <= numberOfTopHitsPerQuerySaved) ? tmpResultsList.size() : numberOfTopHitsPerQuerySaved;
                    tmpResultsList = new ArrayList<ParsedBlastResult>(tmpResultsList.subList(0, tmpSize));
                    queryToHitsMap.put(tmpQueryId, tmpResultsList);
                    worstHitPerQueryMap.put(tmpQueryId, tmpResultsList.get(tmpResultsList.size() - 1));
                }
            }

            // Now create one uber list of hits
            ArrayList<ParsedBlastResult> finalPBRList = new ArrayList<ParsedBlastResult>();
            for (String tmpQueryId : queryToHitsMap.keySet()) {
                finalPBRList.addAll(queryToHitsMap.get(tmpQueryId));
            }
            // Sort the total list by evalue
            Collections.sort(finalPBRList);

            // Loop through and only grab the deflines which matter
            // NOTE: If we need to decrease the memory footprint further, we can remove deflines from deflineMap and not
            // create finalDeflineMap
            Map<String, String> finalDeflineMap = new HashMap<String, String>();
            for (ParsedBlastResult parsedBlastResult : finalPBRList) {
                finalDeflineMap.put(parsedBlastResult.queryId, deflineMap.get(parsedBlastResult.queryId));
                finalDeflineMap.put(parsedBlastResult.subjectId, deflineMap.get(parsedBlastResult.subjectId));
            }

            // Format the final collection object
            referencePBRC.setDeflineMap(finalDeflineMap);
            referencePBRC.setPbrList(finalPBRList);

            // If you got this far, then wipe out the object files.  Not too critical if this fails
            try {
                for (File file : objectFileList) {
                    System.out.println("Deleting object file: " + file.getName());
                    boolean deleteSuccess = file.delete();
                    if (!deleteSuccess){
                        System.err.println("Unable to delete object file "+file.getAbsolutePath());
                    }
                }
            }
            catch (Exception e) {
                System.out.println("Error trying to clean up the " + BlastResultFileNode.DEFAULT_SERIALIZED_PBRC_FILE_EXTENSION + " files.  These may be removed manually without consequence.");
            }
        }
        finally {
            try {
                if (null != ois) {
                    ois.close();
                }
                if (null != fis) {
                    fis.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("The returned PBRC has a list of " + referencePBRC.getParsedBlastResults().size() + " results now.");
        return referencePBRC;
    }

    /**
     * This method is here because NFS cannot reliably have the files ready for access once another process has
     * created them.  It gives NFS a maximum of 10 seconds.
     *
     * @param maximumFileReadyWaitTimeInSeconds
     *                how long we wait for nfs to vouchesafe the file
     * @param tmpFile reference to a file we are waiting for
     * @throws IOException could not access the file
     */
    private void waitTillFileIsReady(File tmpFile, int maximumFileReadyWaitTimeInSeconds) throws IOException {
        if (null == tmpFile) throw new IOException("File referenced in the blast result list is null");

        // if the file is still being written to NFS caching is probably misconfigured
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.S");
        int millisecondWait = (maximumFileReadyWaitTimeInSeconds >= 30) ? 500 : 250;
        int loopIterations = maximumFileReadyWaitTimeInSeconds * 1000 / millisecondWait;

        for (int i = 0; i < loopIterations; i++) {
            Date now = new Date();
            if (tmpFile.exists() && 500 < (now.getTime() - tmpFile.lastModified())) {
                return;
            }
            else {
                try {
                    System.out.println("\nWaiting for file " + tmpFile.getName() + "...");
                    System.out.println("   File " + (tmpFile.exists() ? "does" : "does not") + " exist");
                    System.out.println("   Current time: " + dateFormat.format(now) + " and last modified time is " + dateFormat.format(tmpFile.lastModified()));
                    Thread.sleep(millisecondWait);
                }
                catch (InterruptedException e) {
                    // Do nothing.
                }
            }
        }
        throw new IOException("Unable to access blast result file " + tmpFile);
    }


    /**
     * Filter to look for serialized ParsedBlastResultCollection objects.  The latter objects contain blast hits and
     * any relevant deflines.
     */
    public static class MyFilenameFilter implements FilenameFilter {
        public MyFilenameFilter() {
        }

        public boolean accept(File dir, String name) {
            return null != name && name.endsWith(BlastResultFileNode.DEFAULT_SERIALIZED_PBRC_FILE_EXTENSION);
        }
    }


    /**
     * This main is used by the SGE nodes to execute the merge/sort action.
     * One operation converts a regular xml-based blast output to a serialized ParsedBlastResultCollection object
     * The other operation performs an incremental merge/sort for a secondary grid pass - ie there are so many
     * result files that we need to have nodes on the grid merge/sort subsets of them to simplify the final merge/sort
     *
     * @param args command line arguments which will determine merge/sort function
     */
    public static void main(String[] args) {
        // Not very flexible but who cares, this isn't for general consumption
        try {
            if (args.length == 2
                    && args[0].equals("-o")) {
                BlastGridMergeSort bgms = new BlastGridMergeSort(args[1]);
                bgms.translateSingleOutputToObjects();
            }
            else if (args.length == 3
                    && args[0].equals("-o")
                    && args[2].equals("-full")) {
                String blastResultFile = args[1];
                BlastGridMergeSort bgms = new BlastGridMergeSort(blastResultFile);
                ParsedBlastResultCollection parsedBlastResultCollection =
                        bgms.translateSingleOutputToObjects();
                writeCollectionResults(parsedBlastResultCollection, blastResultFile);
            }
            else if (args.length == 8
                    && args[0].equals("-t")
                    && args[2].equals("-f")
                    && args[4].equals("-l")
                    && args[6].equals("-n")) {
                BlastGridMergeSort bgms = new BlastGridMergeSort(args[1], Integer.parseInt(args[3]), Integer.parseInt(args[5]), Integer.parseInt(args[7]));
                bgms.mergeAndSortObjectResults(10, null);
            }
            else {
                usage(args);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeCollectionResults(ParsedBlastResultCollection parsedBlastResultCollection,
                                               String resultsOutputFilePrefix) throws Exception {

        if (parsedBlastResultCollection != null) {
            System.out.println("\nWriting ParsedBlastResultCollection total and query hit count" +
                    " to " + resultsOutputFilePrefix + " data files");

            // write the total hit count
            File totalHitCountFile = new File(resultsOutputFilePrefix + BLAST_HIT_COUNT_SUFFIX);
            if (!totalHitCountFile.exists()) {
                totalHitCountFile.createNewFile();
            }
            BlastSharedUtil.writeTotalBlastHits(parsedBlastResultCollection,
                    totalHitCountFile.getAbsolutePath());

            // write the query hit count
            File queryCountFile = new File(resultsOutputFilePrefix + QUERY_HIT_COUNT_SUFFIX);
            Set<String> queriesWithHits = new HashSet<String>();
            for (ParsedBlastResult parsedBlastResult : parsedBlastResultCollection.getParsedBlastResults()) {
                queriesWithHits.add(parsedBlastResult.queryId);
            }
            BlastSharedUtil.writeLongValueToNewFile(queryCountFile, queriesWithHits.size());
        }
        else {
            System.out.println("\nInput ParsedBlastResultCollection is null, so no total and query hit count " +
                    " data to write to " + resultsOutputFilePrefix + " data files");
        }


    }

    public static void usage(String[] args) {
        String classname = BlastGridMergeSort.class.getCanonicalName();

        StringBuffer argsString = new StringBuffer();
        for (String arg : args) {
            argsString.append(" ");
            if (arg.matches(".*\\s+.*")) {
                argsString.append("\"").append(arg).append("\" ");
            }
            else {
                argsString.append(arg);
            }
        }

        System.out.println("Invalid usage request: java " + classname + argsString + "\n");
        System.out.println("If parsing the result of a new blast output file, ");
        System.out.println("usage: java " + classname + " -o <blast results file name - full path>\n");
        System.out.println("If parsing the result of a new blast output file and writing out its query and total hit count, ");
        System.out.println("usage: java " + classname + " -o <blast results file name - full path> -full\n");
        System.out.println("If merging and sorting blast output array lists,");
        System.out.println("usage: java " + classname + " -t <blast results dir> -f <first partition processed> -l <last partition processed> -n <number of top hits saved - ranked by e-Value>\n");
        System.out.println("The ordering of the flags is important.");
        System.exit(1);
    }

}
