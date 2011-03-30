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

package org.janelia.it.jacs.model.user_data.blast;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.genomics.BlastHit;
import org.janelia.it.jacs.model.user_data.Node;

import java.io.Serializable;
import java.util.*;

//import org.apache.log4j.*;

/**
 * Created by IntelliJ IDEA.
 * User: kli
 * Date: Dec 1, 2006
 * Time: 2:29:09 PM
 */
public class BlastResultNode extends Node implements Serializable, IsSerializable {

    static Logger logger = Logger.getLogger(BlastResultNode.class.getName());

    private Set<BlastHit> blastHitResultSet = new HashSet<BlastHit>();
    private Map<String, String> deflineMap;
    static int OUTPUT_WIDTH = 80;
    static int INDENT_DEPTH = 12;
    static int SUMMARY_WIDTH = 64;
    static String SPACES = "                                                                               ";

//    private String version;
//    private String reference;
//    private String databaseName;
//    private Integer effectiveSearchSpace;
//    private Integer effectiveHSPLength;
//    private Integer k;
//    private Integer lambda;
//    private Integer h;

    public String toString() {
        String s;
        s = "BlastResultNode: " +
                "NumBlastHits: " +
                (blastHitResultSet != null ? String.valueOf(blastHitResultSet.size()) : "<undefined>") + " " +
                "NumUniqueIDs: " +
                (deflineMap != null ? String.valueOf(deflineMap.size()) : "<undefined>") + "\n";
        return s;
    }

    public BlastResultNode() {
    }

    public Set<BlastHit> getBlastHitResultSet() {
        return blastHitResultSet;
    }

    /**
     * @param resultSet list of blast hits sorted by e-value
     */
    public void setBlastHitResultSet(Set<BlastHit> resultSet) {
        this.blastHitResultSet = resultSet;
    }

    /* Generate a map from query id to blast hit sets.
        The blast hits are sorted within each set which have
        their query id in common
     * @return map of String, SortedSet
     */
    public Map<String, Map<String, SortedSet<BlastHit>>> getSubjectGroupedAndContentSortedMap() {
        long start = System.currentTimeMillis();
        // Map <query_id, subject_id>
        Map<String, Map<String, SortedSet<BlastHit>>> qid_to_bh_map = new HashMap<String, Map<String, SortedSet<BlastHit>>>();
        int numhits = 0;

        for (Object aBlastHitResultSet : blastHitResultSet) {

            BlastHit bh = (BlastHit) aBlastHitResultSet;

            String queryId = bh.getQueryAcc();
            String subjectId = bh.getSubjectAcc();

            Map<String, SortedSet<BlastHit>> sid_to_bh_map;
            if (!qid_to_bh_map.containsKey(queryId)) {
                // if New subject found
                sid_to_bh_map = new HashMap<String, SortedSet<BlastHit>>();
                qid_to_bh_map.put(queryId, sid_to_bh_map);
            }
            else {
                // Prior subject found
                sid_to_bh_map = qid_to_bh_map.get(queryId);
            }

            SortedSet<BlastHit> bhSet;
            if (!sid_to_bh_map.containsKey(subjectId)) {
                bhSet = new TreeSet<BlastHit>();
                sid_to_bh_map.put(subjectId, bhSet);
            }
            else {
                bhSet = sid_to_bh_map.get(subjectId);
            }

            bhSet.add(bh);
            numhits++;
        }

        logger.debug("Num hits added: " + numhits);

        long stop = System.currentTimeMillis();
        if (logger.isInfoEnabled()) logger.info("Time to do getSubjectGroupedAndContentSortedMap: " + (stop - start));
        return qid_to_bh_map;
    }


    /**
     * @return deflineMap string, string map of camera accession to defline
     */
    public Map<String, String> getDeflineMap() {
        return deflineMap;
    }

    /* Given a File, get a map of "query ids-to-hits", and
        iterate over each query id, outputing the blast hits.
     */
//    public void outputBlastText(File outFile) throws Exception{
//
//        Map qid_to_bh_map = getMapFrom_QueryID_to_BlastHit();
//
//        SortedSet sortedKeySet = new TreeSet();
//        sortedKeySet.addAll(qid_to_bh_map.keySet());
//
//        logger.info("Size of sorted key set: "+ sortedKeySet.size());
//
//        FileWriter fw = new FileWriter(outFile);
//        for(Iterator qid_it = sortedKeySet.iterator(); qid_it.hasNext();){
//            String qid = (String)qid_it.next();
//            fw.write("Query= "+ wrapDefline((String)deflineMap.get(qid), new Integer(OUTPUT_WIDTH), new Integer(0))+"\n");
//            fw.write("         ("+((BlastHit)((SortedSet)qid_to_bh_map.get(qid)).first()).getQueryLength()+" letters)\n");
//            outputBlastHitSet(fw, (SortedSet)qid_to_bh_map.get(qid));
//        }
//        fw.close();
//    }

    /* For a sorted set of blast results (which are assumed to have the same query id,
        write out a summary and the HSPs to the FileWriter.
     */
//    private void outputBlastHitSet(FileWriter fw, SortedSet SortedBlastHitSet) throws Exception{
//        Iterator it;
//
//        DecimalFormat bitScoreDF=new DecimalFormat("0");
//        Integer bitScoreWdt=new Integer(5);
//        DecimalFormat evalDF=new DecimalFormat("0.00");
//        Integer evalWdt=new Integer(6);
//
//        fw.write("                                                                 Score    E\n");
//        fw.write("Sequences producing significant alignments:                      (bits) Value\n\n");
//
//        /* Print Summaries */
//        for(it=SortedBlastHitSet.iterator(); it.hasNext();){
//            BlastHit bh=(BlastHit)it.next();
//
//            // Shorten the defline
//            // This has been known to throw null pointers and MUST BE FIXED!!!
//            String defline=(String)deflineMap.get(bh.getSubjectAcc());
//            if
//            if(defline.length()>SUMMARY_WIDTH){
//                defline = defline.substring(0, SUMMARY_WIDTH);
//                defline += "...";
//            }else{
//                defline += SPACES.substring(0, SUMMARY_WIDTH - defline.length());
//            }
//
//            // Format the bit score
//            String bsStr=bitScoreDF.format(bh.getBitScore().doubleValue());
//            if(bsStr.length()<bitScoreWdt.intValue()){
//                bsStr=SPACES.substring(0, bitScoreWdt.intValue()-bsStr.length())+bsStr;
//            }
//
//            // Format the E Value
//            String evalStr=evalDF.format(bh.getExpectScore().doubleValue());
//             if(evalStr.length()<evalWdt.intValue()){
//                evalStr=SPACES.substring(0, evalWdt.intValue()-evalStr.length())+evalStr;
//            }
//
//            fw.write(defline+ " " + bsStr + " " + evalStr + "\n");
//        }
//
//        fw.write("\n\n");
//
//
//        /* Print HSPs */
//        for(it=SortedBlastHitSet.iterator(); it.hasNext();){
//            BlastHit bh=(BlastHit)it.next();
//            fw.write(">" + wrapDefline((String)deflineMap.get(bh.getSubjectAcc()), new Integer(OUTPUT_WIDTH), new Integer(INDENT_DEPTH))+"\n");
//            fw.write("          Length = "+bh.getSubjectLength().toString()+"\n\n");
//            fw.write(bh.getHitStatistics()+"\n\n");
//            fw.write(bh.getPairWiseAlignment(new Integer(80)).toString()+"\n\n");
//        }
//
//    }

    // Determine which Query IDs had hits

    public Set<String> getHitQuerySet() {
        Set<String> hitSet = new HashSet<String>();
        for (Object aBlastHitResultSet : this.blastHitResultSet) {
            hitSet.add(((BlastHit) aBlastHitResultSet).getQueryAcc());
        }
        return hitSet;
    }

    // Determine which Subject IDs got hit
    public Set<String> getHitSubjectSet() {
        Set<String> hitSet = new HashSet<String>();
        for (Object aBlastHitResultSet : this.blastHitResultSet) {
            hitSet.add(((BlastHit) aBlastHitResultSet).getSubjectAcc());
        }
        return hitSet;
    }

    // Set the map to determine the deflines to be associated with sequences id's
    /**
     * @param deflineMap string, string map of camera accession to defline
     */
    public void setDeflineMap(Map<String, String> deflineMap) {
        this.deflineMap = deflineMap;
    }

//    public static void main(String[] args){

//        logger.addAppender(new ConsoleAppender(new SimpleLayout()));

    // Test the defline/wrap formating
//        String defline = "Query= NCBI_PEP_2695847 /offset=0 /translation_table=unknown /length=186 /full_length=186 /accession=CAA73704.1 /description=\"immunoglobulin heavy chain [Acipenser baerii]\"";
//        defline= wrapDefline(defline, OUTPUT_WIDTH, INDENT_DEPTH);
//        logger.debug(defline);


    // Test the padding of decimal values
//        double rnd;
//        String fmt = "0.00";
//        DecimalFormat df = new DecimalFormat(fmt);
//        for(int i=0; i<40; i++){
//            rnd=Math.random()*10;
//            String s =df.format(rnd);
//            if(s.length()<7){
//                s=SPACES.substring(0, 7-s.length())+s;
//            }
//
//            logger.debug(s);
//        }

//    }
}
