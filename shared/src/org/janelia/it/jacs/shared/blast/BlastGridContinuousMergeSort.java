
package org.janelia.it.jacs.shared.blast;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.blast.*;
//import org.janelia.it.jacs.model.tasks.psiBlast.ReversePsiBlastTask;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Apr 2, 2008
 * Time: 2:05:08 PM
 */
public class BlastGridContinuousMergeSort {

    private File blastResultDir;
    private int numberOfTopHitsPerQuerySaved;
    private int numberOfPartitions;
    private int NEW_OOS_WAIT_PERIOD_MS = 60000;  // one-minute
    private long mostRecentFileTime = new Date().getTime();
    private long queryCountWithHits = 0L;

    public BlastGridContinuousMergeSort(String blastResultDir, int numberOfPartitions,
                                        int numberOfTopHitsPerQuerySaved) {
        this.blastResultDir = new File(blastResultDir);
        this.numberOfTopHitsPerQuerySaved = numberOfTopHitsPerQuerySaved;
        this.numberOfPartitions = numberOfPartitions;
    }

    /**
     * This method is able to loop through xml blast output files and sort/truncate hits based on score and
     * other blast parameters.  I refer to ParsedBlastResults as "hits".
     *
     * @param maximumFileReadyWaitTimeInSeconds
     *                  - how long we wait for nfs to vouchesafe the file
     * @param maximumNewFileWaitTimeInSeconds
     *                  - max time to wait for the new file
     * @param blastTask - task we're running at the moment
     * @return ParsedBlastResultCollection which contains hits or all queries and deflines for all subjects and queries
     * @throws IOException            could not access one of the blast output files
     * @throws ClassNotFoundException could not read in the object stream. there should at least be an empty collection
     */
    public ParsedBlastResultCollection mergeAndSortObjectResults(int maximumFileReadyWaitTimeInSeconds,
                                                                 int maximumNewFileWaitTimeInSeconds,
                                                                 Task blastTask)
            throws IOException, ClassNotFoundException {
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        Map<String, ArrayList<ParsedBlastResult>> queryToHitsMap = new HashMap<>();
        Map<String, ParsedBlastResult> worstHitPerQueryMap = new HashMap<>();
        Map<String, String> deflineMap = new HashMap<>();
        ParsedBlastResultCollection referencePBRC = new ParsedBlastResultCollection();

        if (blastTask != null && blastTask.getMessages().size() > 0) {
            throw new RuntimeException("Blast task unexpectedly contains messages: " + blastTask.getMessages());
        }

        // Begin loop waiting for result files
        HashSet<File> partitionsProcessed = new HashSet<>();
        try {
            while (partitionsProcessed.size() < numberOfPartitions) {
                long currentTime = new Date().getTime();
                if (currentTime - mostRecentFileTime > (maximumNewFileWaitTimeInSeconds * 1000)) {
                    throw new IOException("Max new file wait time exceeded");
                }
                File[] objectFileArr = blastResultDir.listFiles(new BlastParsedObjectStreamFilenameFilter());
                ArrayList<File> objectFileList = new ArrayList<>();
                for (File f : objectFileArr)
                    if (!partitionsProcessed.contains(f))
                        objectFileList.add(f);
                if (objectFileList.size() > 0) {
                    // Reset most recent file wait time
                    mostRecentFileTime = new Date().getTime();
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
                            tmpResultsList = new ArrayList<>(tmpResultsList.subList(0, tmpSize));
                            queryToHitsMap.put(tmpQueryId, tmpResultsList);
                            worstHitPerQueryMap.put(tmpQueryId, tmpResultsList.get(tmpResultsList.size() - 1));
                        }
                        partitionsProcessed.add(file);
                    }
                }
                if (partitionsProcessed.size() < numberOfPartitions) {
                    System.out.println("BlastGridContinuousMergeSort waiting for oos files...");
                    try {
                        Thread.sleep(NEW_OOS_WAIT_PERIOD_MS);
                    }
                    catch (InterruptedException ex) {
                        // Waiting for the thread sleep to break
                    }
                }
            }

            // Now create one uber list of hits
            ArrayList<ParsedBlastResult> finalPBRList = new ArrayList<>();
            for (String tmpQueryId : queryToHitsMap.keySet()) {
                finalPBRList.addAll(queryToHitsMap.get(tmpQueryId));
            }
            // Sort the total list by evalue
            Collections.sort(finalPBRList);

            // Loop through and only grab the deflines which matter
            // NOTE: If we need to decrease the memory footprint further, we can remove deflines from deflineMap and not
            // create finalDeflineMap
            Map<String, String> finalDeflineMap = new HashMap<>();
            Set<String> queriesWithHits = new HashSet<>();
            for (ParsedBlastResult parsedBlastResult : finalPBRList) {
                if (!queriesWithHits.contains(parsedBlastResult.queryId))
                    queriesWithHits.add(parsedBlastResult.queryId);
                finalDeflineMap.put(parsedBlastResult.queryId, deflineMap.get(parsedBlastResult.queryId));
                finalDeflineMap.put(parsedBlastResult.subjectId, deflineMap.get(parsedBlastResult.subjectId));
            }
            queryCountWithHits = queriesWithHits.size();

            // Format the final collection object
            referencePBRC.setDeflineMap(finalDeflineMap);
            referencePBRC.setPbrList(finalPBRList);

            // If you got this far, then wipe out the object files.  Not too critical if this fails
            try {
                for (File file : partitionsProcessed) {
                    System.out.println("Deleting object file: " + file.getName());
                    boolean deleteSuccess = file.delete();
                    if (!deleteSuccess){
                        System.out.println("Unsuccessfully tried to delete "+file.getName()+". Continuing...");
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
        throw new IOException("Unable to access blast result file " + tmpFile + ".  Ensure no literal time difference between computeserver and grid nodes." +
                "\nCheck blastGridMerge_Error.x files for timing errors.");
    }

    public long getQueryCountWithHits() {
        return queryCountWithHits;
    }

    public static void copyBlastTaskToDir(IBlastOutputFormatTask blastTask, File dir) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dir.getAbsolutePath()
                + File.separator + BlastResultFileNode.PARSED_BLAST_RESULTS_COLLECTION_BASENAME + ".blastTask"));
        try {
//            IBlastOutputFormatTask originalTask = (IBlastOutputFormatTask) blastTask;
            IBlastOutputFormatTask parameterBlastTask = createParameterBlastTask(blastTask);
            oos.writeObject(parameterBlastTask);
        }
        finally {
            oos.flush();
            oos.close();
        }
    }

    // This method only copies the parameters of the source blast task. The purpose is to create a clean non-hiberate
    // object that can be serialized to the filesystem for use by the blast parser.
    private static IBlastOutputFormatTask createParameterBlastTask(IBlastOutputFormatTask blastTask) {
        IBlastOutputFormatTask pt;
        if (blastTask instanceof BlastPTask) {
            pt = new BlastPTask();
        }
        else if (blastTask instanceof BlastXTask) {
            pt = new BlastXTask();
        }
        else if (blastTask instanceof MegablastTask) {
            pt = new MegablastTask();
        }
        else if (blastTask instanceof TBlastNTask) {
            pt = new TBlastNTask();
        }
        else if (blastTask instanceof TBlastXTask) {
            pt = new TBlastXTask();
        }
//        else if (blastTask instanceof ReversePsiBlastTask) {
//            pt = (IBlastOutputFormatTask)new ReversePsiBlastTask();
//        }
        else {
            pt = new BlastNTask();
        }
        Set<String> parameterKeySet = blastTask.getParameterKeySet();
        for (String k : parameterKeySet) {
            String v = blastTask.getParameter(k);
            pt.setParameter(k, v);
        }
        return pt;
    }

    /**
     * Filter to look for serialized ParsedBlastResultCollection objects.  The latter objects contain blast hits and
     * any relevant deflines.
     */
    public static class BlastParsedObjectStreamFilenameFilter implements FilenameFilter {
        public BlastParsedObjectStreamFilenameFilter() {
        }

        public boolean accept(File dir, String name) {
            return null != name && name.endsWith(BlastResultFileNode.DEFAULT_SERIALIZED_PBRC_FILE_EXTENSION);
        }
    }

    public static synchronized void writeParsedBlastResultCollectionToFs(
            ParsedBlastResultCollection parsedBlastResultCollection,
            File blastDestOutputDir, long queryCountWithHits) throws IOException {

        // writing results first, so failure to write counts will not cause loss of results
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(blastDestOutputDir.getAbsolutePath() + File.separator + BlastResultFileNode.PARSED_BLAST_RESULTS_COLLECTION_FILENAME));
        oos.writeObject(parsedBlastResultCollection);
        oos.close();

        System.out.println("writing queryCount file queryCountWithHits=" + queryCountWithHits);
        File queryCountFile = new File(blastDestOutputDir.getAbsolutePath() + File.separator + BlastResultFileNode.PARSED_BLAST_RESULTS_COLLECTION_BASENAME + ".queryCountWithHits");
        BlastSharedUtil.writeLongValueToNewFile(queryCountFile, queryCountWithHits);

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
        System.out.println("Starting BlastGridContinuousMergeSort v2...");
        try {
            if (args.length == 4) {
                BlastGridContinuousMergeSort bgms = new BlastGridContinuousMergeSort(
                        args[0], // blast result directory
                        Integer.parseInt(args[1]), // number of partitions
                        Integer.parseInt(args[2])); // number of top hits per query
                String totalHitCountFilePath = args[3];
                ParsedBlastResultCollection parsedBlastResultCollection = bgms.mergeAndSortObjectResults(
                        120, // seconds for file to be finished
                        172800, // seconds for new file to appear - 172800 = 2 days
                        null); // task is null for command-line case
                writeParsedBlastResultCollectionToFs(parsedBlastResultCollection, new File(args[0]), bgms.getQueryCountWithHits());
                BlastSharedUtil.writeTotalBlastHits(parsedBlastResultCollection, totalHitCountFilePath);
            }
            else {
                usage();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            System.exit(1);
        }
        System.out.println("Finished BlastGridContinuousMergeSort v2.");
    }

    public static void usage() {
        System.out.println("usage: <blast result dir path> <number of partitions> <max num hits per query> <total hit count filepath>");
        System.exit(1);
    }

}
