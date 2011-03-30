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

package org.janelia.it.jacs.shared.blast.blastxmlparser;

import org.janelia.it.jacs.shared.blast.ParsedBlastHSP;
import org.janelia.it.jacs.shared.blast.ParsedBlastResult;
import org.janelia.it.jacs.shared.blast.ParsedBlastResultCollection;
import org.janelia.it.jacs.shared.blast.blastxmlparser.jaxb.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;


/**
 * Created by IntelliJ IDEA.
 * User: kli
 * Date: Dec 21, 2006
 * Time: 11:39:38 AM
 */
public class BlastXMLParser {

    private BlastOutputType jbBlastOutput;
    private ParsedBlastResultCollection pbrCollection = new ParsedBlastResultCollection();

    enum SequenceType {
        NUCLEOTIDE, PEPTIDE
    }

    int FORWARD = 1;
    int REVERSE = -1;
    // NCBI data is coming in without spaces in the defline
    // there are some deflines that begin with > - need to skip it
    private static Pattern IDPattern = Pattern.compile("^\\>?([^\\s/]+)");
//    private static Pattern IDPattern = Pattern.compile("^([^\\s/]+)");

    JAXBContext jaxbContext = null;

    public BlastXMLParser() throws Exception {
        jaxbContext = JAXBContext.newInstance("org.janelia.it.jacs.shared.blast.blastxmlparser.jaxb");
    }

    public void loadXML(File blastResultFile) throws javax.xml.bind.JAXBException {
        if (blastResultFile.length() > 0) {
            //Unmarshaller unmarshaller = jbObjectFactory.createUnmarshaller();
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            //unmarshaller.setValidating(false);
            JAXBElement<BlastOutputType> jbBlastOutputElement = (JAXBElement<BlastOutputType>) unmarshaller.unmarshal(blastResultFile);
            jbBlastOutput = jbBlastOutputElement.getValue();
        }
        else {
            jbBlastOutput = null;
        }
    }

    public void transferToParsedBlastResultSet() throws Exception {
        transferToParsedBlastResultSet(true);
    }

    public void transferToParsedBlastResultSet(boolean normalizeDeflines) throws Exception {

        if (jbBlastOutput == null)
            return;

        ParsedBlastResult pbr = new ParsedBlastResult();
        SequenceType seqType;

        pbr.programUsed = jbBlastOutput.getBlastOutputProgram();
        pbr.setBlastVersion(jbBlastOutput.getBlastOutputVersion());
        if (pbr.programUsed.equals("blastn") || pbr.programUsed.equals("megablast")) {
            seqType = SequenceType.NUCLEOTIDE;
        }
        else {
            seqType = SequenceType.PEPTIDE;
        }

        Iteration jbIteration = jbBlastOutput.getBlastOutputIterations();
        List itList = jbIteration.getIteration();
        Iterator itIt;
        for (itIt = itList.iterator(); itIt.hasNext();) {

            IterationType curIt = (IterationType) itIt.next();

            // Only parse if there are hits
            if (curIt.getIterationHits() != null) {

                pbr.queryId = extractID(curIt.getIterationQueryDef());
                if (normalizeDeflines) {
                    pbrCollection.addDefline(pbr.queryId, curIt.getIterationQueryDef());
                }
                else {
                    pbr.setQueryDefline(curIt.getIterationQueryDef());
                }
                pbr.queryLength = curIt.getIterationQueryLen().intValue();
                pbr.comment = curIt.getIterationMessage();

                Hit jbHits = curIt.getIterationHits();
                List hitList = jbHits.getHit();

                Iterator hitIt;
                for (hitIt = hitList.iterator(); hitIt.hasNext();) {

                    HitType curHit = (HitType) hitIt.next();
                    pbr.subjectId = extractID(curHit.getHitDef());
                    pbr.subjectLength = curHit.getHitLen().intValue();
                    if (normalizeDeflines) {
                        pbrCollection.addDefline(pbr.subjectId, curHit.getHitDef());
                    }
                    else {
                        pbr.setSubjectDefline(curHit.getHitDef());
                    }
                    Hsp jbHsp = curHit.getHitHsps();
                    List hspList = jbHsp.getHsp();

                    List<ParsedBlastHSP> parsedHSPs = new ArrayList<ParsedBlastHSP>();
                    Iterator hspIt;
                    for (hspIt = hspList.iterator(); hspIt.hasNext();) {

                        HspType curHsp = (HspType) hspIt.next();

                        ParsedBlastHSP hsp = new ParsedBlastHSP();
                        hsp.setHspOrd(curHsp.getHspNum().longValue());

                        hsp.setQueryBegin(curHsp.getHspQueryFrom().intValue());
                        hsp.setQueryEnd(curHsp.getHspQueryTo().intValue());

                        if (curHsp.getHspQueryFrame() != null) {
                            hsp.setQueryFrame(curHsp.getHspQueryFrame().intValue());
                        }
                        else {
                            hsp.setQueryFrame(FORWARD);
                        }

                        hsp.setSubjectBegin(curHsp.getHspHitFrom().intValue());
                        hsp.setSubjectEnd(curHsp.getHspHitTo().intValue());

                        if (curHsp.getHspHitFrame() != null) {
                            hsp.setSubjectFrame(curHsp.getHspHitFrame().intValue());
                        }
                        else {
                            hsp.setSubjectFrame(FORWARD);
                        }

                        // set orientation based on frame value since this appears to always
                        // produce the proper sequence orientation regardless of blast program
                        hsp.setQueryOrientation(getOrientation(hsp.getQueryFrame()));
                        if (hsp.getQueryBegin() > hsp.getQueryEnd()) {
                            Integer tmp = hsp.getQueryBegin();
                            hsp.setQueryBegin(hsp.getQueryEnd());
                            hsp.setQueryEnd(tmp);
                        }
                        hsp.setQueryBegin(hsp.getQueryBegin() - 1);

                        // set orientation based on frame value since this appears to always
                        // produce the proper sequence orientation regardless of blast program
                        hsp.setSubjectOrientation(getOrientation(hsp.getSubjectFrame()));
                        if (hsp.getSubjectBegin() > hsp.getSubjectEnd()) {
                            Integer tmp = hsp.getSubjectBegin();
                            hsp.setSubjectBegin(hsp.getSubjectEnd());
                            hsp.setSubjectEnd(tmp);
                        }
                        hsp.setSubjectBegin(hsp.getSubjectBegin() - 1);

                        hsp.setExpectScore(curHsp.getHspEvalue());
                        hsp.setBitScore(curHsp.getHspBitScore());
                        hsp.setHspScore(curHsp.getHspScore());
                        hsp.setLengthAlignment(curHsp.getHspAlignLen().intValue());
                        hsp.setEntropy(entropy(curHsp.getHspMidline(), hsp.getLengthAlignment().longValue()));

                        if (curHsp.getHspIdentity() != null) {
                            hsp.setNumberIdentical(curHsp.getHspIdentity().intValue());
                        }
                        else {
                            hsp.setNumberIdentical(0);
                        }

                        if (curHsp.getHspPositive() != null && curHsp.getHspIdentity() != null) {
                            hsp.setNumberSimilar(curHsp.getHspPositive().intValue() - curHsp.getHspIdentity().intValue());
                        }
                        else {
                            hsp.setNumberSimilar(0);
                        }

                        hsp.setQueryAlignString(curHsp.getHspQseq());
                        AlignmentCounts qryAC = new AlignmentCounts(curHsp.getHspQseq(), seqType);
                        hsp.setQueryGaps(qryAC.getGaps());
                        hsp.setQueryGapRuns(qryAC.getGapRuns());
                        hsp.setQueryStops(qryAC.getStops());
                        hsp.setQueryNumberUnalignable(qryAC.getUnalignable());

                        hsp.setSubjectAlignString(curHsp.getHspHseq());
                        AlignmentCounts subAC = new AlignmentCounts(curHsp.getHspHseq(), seqType);
                        hsp.setSubjectGaps(subAC.getGaps());
                        hsp.setSubjectGapRuns(subAC.getGapRuns());
                        hsp.setSubjectStops(subAC.getStops());
                        hsp.setSubjectNumberUnalignable(subAC.getUnalignable());

                        hsp.setMidline(curHsp.getHspMidline());

                        parsedHSPs.add(hsp);
                    }
                    pbr.setHspList(parsedHSPs);
                    pbrCollection.addParsedBlastResult((ParsedBlastResult) pbr.clone());
                }
            }
        }

//        ("Parsed Blast Result Set Size: " + pbrCollection.size());
    }

    /*
        determines sequence orientation based on frame value
             positive frames indicate forward sequence orientations
             negative frames indicate reverse sequence orientations
        this seems to hold true regardless of the blast program used
     */
    private Integer getOrientation(Integer frame) {
        if (frame != null && frame < 0) {
            return REVERSE;
        }
        else {
            return FORWARD;
        }
    }

    public ParsedBlastResultCollection getParsedBlastResultCollection() {
        return pbrCollection;
    }

    public void setPbrSet(ParsedBlastResultCollection pbrCollection) {
        this.pbrCollection = pbrCollection;
    }

    private Float entropy(String matchString, Long alignmentLength) {
        // Computes the entropy of the given string based on non *, + and space
        //  characters.
        // matchString should look something like:
        // ++ TD  +       E T ++PG   +S GA+VR LG    G + + +   T  +  +T

        Integer x;
        HashMap<String, Integer> charFreqMap = new HashMap<String, Integer>();
        final float log2 = (float) Math.log(2);

        // If nucleotide match
        matchString = matchString.replaceAll("\\|", "");
        if (matchString.length() == 0) return null;

        // If peptide match
        matchString = matchString.replaceAll("\\s+", "");
        matchString = matchString.replaceAll("[\\+\\*]", "");

        Integer strlen = matchString.length();

        // Compute frequencies of each char in the matchString
        for (x = 0; x < strlen; x++) {
            String theChar = matchString.charAt(x) + "";

            if (!charFreqMap.containsKey(theChar)) {
                charFreqMap.put(theChar, 1);
            }
            else {
                Integer count = charFreqMap.get(theChar);
                count++;
                charFreqMap.put(theChar, count);
            }
        }

        // Loop over each bin and compute entropy contributions
        Set<String> keySet = charFreqMap.keySet();
        String key;
        float entropy = 0F;

        for (String aKeySet : keySet) {
            key = aKeySet;
            float freq = charFreqMap.get(key).floatValue() / alignmentLength.floatValue();
            entropy += freq * Math.log(freq) / log2;
        }

        if (entropy != 0.0F) {
            entropy = -entropy;
        }

        return entropy;
    }

    private String extractID(String inLine) throws Exception {
        // Extracts what's in the paren's of thePattern that matches in the input line
        Scanner lineScanner = new Scanner(inLine);
        try {
            String foundLine = lineScanner.findInLine(IDPattern);
            if (foundLine != null) {
                MatchResult theMatchResult = lineScanner.match();
                return (theMatchResult.group(1));
            }
            else {
                throw new Exception("Could not parse sequence ID out of :" + inLine +
                        "\nAre you sure you didn't set the -J option in blastall?");
            }
        }
        finally {
            lineScanner.close();
        }
    }

    public Set<String> getSubjectIDsSet() {
        Set<String> hitSubjectsSet = new HashSet<String>();
        Iterator it;
        for (it = pbrCollection.iterator(); it.hasNext();) {
            ParsedBlastResult pbr = (ParsedBlastResult) it.next();
            hitSubjectsSet.add(pbr.subjectId);
        }
        return hitSubjectsSet;
    }

    public Set<String> getQueryIDsSet() {
        Set<String> hitQueriesSet = new HashSet<String>();
        Iterator it;
        for (it = pbrCollection.iterator(); it.hasNext();) {
            ParsedBlastResult pbr = (ParsedBlastResult) it.next();
            hitQueriesSet.add(pbr.queryId);
        }
        return hitQueriesSet;
    }

//    public static void main(String[] args){
//
//        logger.addAppender(new ConsoleAppender(new SimpleLayout()));
//
//        try{
//            BlastXMLParser bxp = new BlastXMLParser();
//
//            List<File> inputFileList = new ArrayList<File>();
//            File outputFile=null;
//
//            boolean readInput=false;
//            boolean readOutput=false;
//
//            // Read input/output file names from command line
//            for(String arg: args){
//                if(arg.equals("-i")){
//                    readInput=true;
//                    readOutput=false;
//                }else if(arg.equals("-o")){
//                    readOutput=true;
//                    readInput=false;
//                }else{
//                    if(readInput){
//                        inputFileList.add(new File(arg));
//                    }else if(readOutput){
//                        outputFile=new File(arg);
//                    }else{
//                        logger.fatal("Not sure whether arguments are inputs or outputs.");
//                        return;
//                    }
//                }
//            }
//
//            // Dump usage if either input or output files are not specified.
//            if(null==outputFile || inputFileList.isEmpty()){
//                logger.fatal("Usage: -i <input files list...> -o <output file name>");
//                return;
//            }
//
//            Iterator it;
//            for(it=inputFileList.iterator(); it.hasNext();){
//                File theFile=(File)it.next();
//                logger.info("Working on:"+theFile.getName());
//                bxp.loadXML(theFile);
//                bxp.transferToParsedBlastResultSet();
//            }
//            bxp.writeToFile(outputFile);
//
//            logger.info("Num Unique Hit Queries: "+ bxp.getQueryIDsSet().size());
//            logger.info("Num Unique Hit Subjects: "+ bxp.getSubjectIDsSet().size());
//
//        }catch(Exception e){
//            logger.fatal("Exception while trying to parse BLAST XML.", e);
//        }
//    }
//

    private class AlignmentCounts {
        private Integer gaps;
        private Integer gapRuns;
        private Integer stops;
        private Integer unalignable;

        public Integer getGapRuns() {
            return gapRuns;
        }

        public Integer getGaps() {
            return gaps;
        }

        public Integer getStops() {
            return stops;
        }

        public Integer getUnalignable() {
            return unalignable;
        }

        public AlignmentCounts(String theSequence, SequenceType theSeqType) {
            Integer i;
            Integer length = theSequence.length();

            gaps = 0;
            gapRuns = 0;
            stops = 0;
            unalignable = 0;

            for (i = 0; i < length; i++) {
                char theChar = theSequence.charAt(i);

                switch (theChar) {
                    case '-':
                        gaps++;
                        if ((i > 0 && theSequence.charAt(i - 1) != '-') || i == 0) {
                            gapRuns++;
                        }
                        break;
                    case '*':
                        stops++;
                        break;
                    case 'N':
                    case 'n':
                        if (theSeqType == SequenceType.NUCLEOTIDE) {
                            unalignable++;
                        }
                        break;
                    case 'X':
                    case 'x':
                        if (theSeqType == SequenceType.PEPTIDE) {
                            unalignable++;
                        }
                }
            }
        }

        public String toString() {
            String s = "";
            s += "gaps: " + gaps;
            s += "gap runs: " + gapRuns;
            s += "stops: " + stops;
            s += "unalignable: " + unalignable;
            s += "\n";
            return s;
        }
    }
}
