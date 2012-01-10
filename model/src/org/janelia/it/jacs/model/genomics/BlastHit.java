
package org.janelia.it.jacs.model.genomics;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.user_data.Node;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: tdolafi
 * Date: Aug 7, 2006
 * Time: 9:24:49 PM
 */
public class BlastHit implements Serializable, IsSerializable, Comparable {

    public static Integer ALGN_ORI_REVERSE = -1;
    public static Integer ALGN_ORI_UNKNOWN = 0;
    public static Integer ALGN_ORI_FORWARD = 1;

// general values

    private Long blastHitId;
    private String programUsed;
    private String blastVersion;
    private Float bitScore;
    private Float hspScore;
    private Double expectScore;
    private String comment;
    private Integer lengthAlignment;
    private Float entropy;
    private Integer numberIdentical;
    private Integer numberSimilar;

// sequence values

    private BaseSequenceEntity subjectEntity;
    private Integer subjectBegin;
    private Integer subjectEnd;
    private Integer subjectOrientation;
    private BaseSequenceEntity queryEntity;
    private Integer queryBegin;
    private Integer queryEnd;
    private Integer queryOrientation;
    private Node resultNode;
    private Integer rank;

// Subject specific metrics

    private String subjectAcc;
    private Integer subjectLength;
    private Integer subjectGaps;
    private Integer subjectGapRuns;
    private Integer subjectStops;
    private Integer subjectNumberUnalignable;
    private Integer subjectFrame;


    // Query specific metrics
    private Long queryNodeId;
    private String queryAcc;
    private Integer queryLength;
    private Integer queryGaps;
    private Integer queryGapRuns;
    private Integer queryStops;
    private Integer queryNumberUnalignable;
    private Integer queryFrame;

    // Alignment strings
    private String subjectAlignString;
    private String midline;
    private String queryAlignString;

// Methods

    public BlastHit() {
    }

    private Integer countGaps(String seq) {
        int count = 0;
        int seqLen = seq.length();
        for (int i = 0; i < seqLen; i++) {
            count += (seq.charAt(i) == '-') ? 1 : 0;
        }
        return count;
    }

    private Integer max(Integer a, Integer b) {
        return (a > b ? a : b);
    }

    public String getPairWiseAlignment(Integer maxAlignmentWidth) throws Exception {
        return getPairWiseAlignment(maxAlignmentWidth, true, false);
    }

    public String getPairWiseAlignment(Integer maxAlignmentWidth, boolean includeMidline, boolean postProcess) throws Exception {

        String output = "";

        Integer i = 0;
        Integer qOffBegin;
        Integer qOffEnd;
        Integer qOri = getQueryOrientation();
        Integer sOffBegin;
        Integer sOffEnd;
        Integer sOri = getSubjectOrientation();

        if (qOri.equals(BlastHit.ALGN_ORI_FORWARD)) {
            qOffBegin = getQueryBegin();
            qOffEnd = getQueryEnd();
        }
        else if (qOri.equals(BlastHit.ALGN_ORI_REVERSE)) {
            qOffEnd = getQueryBegin();
            qOffBegin = getQueryEnd();
        }
        else {
            throw new Exception("Unknown Query Orientation:  Could not generate PairWiseAlignment");
        }

        if (sOri.equals(BlastHit.ALGN_ORI_FORWARD)) {
            sOffBegin = getSubjectBegin();
            sOffEnd = getSubjectEnd();
        }
        else if (qOri.equals(BlastHit.ALGN_ORI_REVERSE)) {
            sOffEnd = getSubjectBegin();
            sOffBegin = getSubjectEnd();
        }
        else {
            throw new Exception("Unknown Subject Orientation:  Could not generate PairWiseAlignment");
        }

        Integer qOffBegLen = qOffBegin.toString().length();
        Integer qOffEndLen = qOffEnd.toString().length();
        Integer sOffBegLen = sOffBegin.toString().length();
        Integer sOffEndLen = sOffEnd.toString().length();
        Integer maxOffLen = max(qOffBegLen, max(qOffEndLen, max(sOffBegLen, sOffEndLen)));


        Integer sResidue;
        Integer qResidue;
        if (programUsed.equals("blastn")) {
            sResidue = 1;
            qResidue = 1;
        }
        else if (programUsed.equals("blastp")) {
            sResidue = 1;
            qResidue = 1;
        }
        else if (programUsed.equals("blastx")) {
            sResidue = 1;
            qResidue = 3;
        }
        else if (programUsed.equals("tblastn")) {
            sResidue = 3;
            qResidue = 1;
        }
        else if (programUsed.equals("tblastx")) {
            sResidue = 3;
            qResidue = 3;
        }
        else if (programUsed.equals("megablast")) {
            sResidue = 1;
            qResidue = 1;
        }
        else {
            throw new Exception("Program type was not set.  Cannot compute offsets.");
        }


        Integer algnLen = getMidline().length();
        do {
            Integer ub;
            ub = (i + maxAlignmentWidth > algnLen) ? algnLen : i + maxAlignmentWidth;

            String qas = getQueryAlignString().substring(i, ub);
            String sas = getSubjectAlignString().substring(i, ub);
            String mas = getMidline().substring(i, ub);

            Integer qGaps = countGaps(qas);
            Integer sGaps = countGaps(sas);

            Integer sDelta = (ub - i - sGaps) * sResidue;
            Integer qDelta = (ub - i - qGaps) * qResidue;

            if (sOri.equals(BlastHit.ALGN_ORI_REVERSE)) {
                sDelta = sDelta * -1;
            }
            if (qOri.equals(BlastHit.ALGN_ORI_REVERSE)) {
                qDelta = qDelta * -1;
            }


            Integer orbc_qOffBegin = qOffBegin;
            Integer orbc_qOffEnd = qOffBegin + qDelta;
            if (orbc_qOffBegin < orbc_qOffEnd) {
                orbc_qOffBegin = orbc_qOffBegin + 1;
            }
            else {
                orbc_qOffEnd = orbc_qOffEnd + 1;
            }

            Integer orbc_sOffBegin = sOffBegin;
            Integer orbc_sOffEnd = sOffBegin + sDelta;

            if (orbc_sOffBegin < orbc_sOffEnd) {
                orbc_sOffBegin = orbc_sOffBegin + 1;
            }
            else {
                orbc_sOffEnd = orbc_sOffEnd + 1;
            }

            // Pad out the offsets so the sequences and midline still line up.
            String qBeginString = orbc_qOffBegin + " ";
            String qEndString = " " + orbc_qOffEnd;

            String sBeginString = orbc_sOffBegin + " ";
            String sEndString = " " + orbc_sOffEnd;

            while (qBeginString.length() <= maxOffLen) {
                qBeginString += " ";
            }
            while (sBeginString.length() <= maxOffLen) {
                sBeginString += " ";
            }
            String midLineString = "";
            while (midLineString.length() <= maxOffLen) {
                midLineString += " ";
            }

            if (postProcess) {
                String[] strings = processSubAlignment(new String[]{qas, mas, sas});
                qas = strings[0];
                mas = strings[1];
                sas = strings[2];
            }

            // Save the String
            output += ("Query: " + qBeginString + qas + qEndString + "\n" +
                    ((includeMidline) ? "       " + midLineString + mas + "\n" : "") +
                    "Sbjct: " + sBeginString + sas + sEndString + "\n\n");


            i = i + maxAlignmentWidth;
            qOffBegin = qOffBegin + qDelta;
            sOffBegin = sOffBegin + sDelta;

        }
        while (i <= algnLen);

        return output;
    }

    //TODO: move this nasty HTML to some kind of HTML helper processor
    //TODO: use 1 span for every sequence of the same span class, instead of a separate span for each letter
    private String[] processSubAlignment(String[] strings) {
        String query = strings[0];
        String mid = strings[1];
        String subj = strings[2];
        StringBuffer queryOut = new StringBuffer();
        StringBuffer midOut = new StringBuffer();
        StringBuffer subjOut = new StringBuffer();

        for (int i = 0; i < query.length(); i++) {
            char ch = mid.charAt(i);
            if (ch == '|') {
                // Use placeholder for spaces because spaces will get changed to &nbsp;'s
                queryOut.append("<span%%class='alignmentMatch'>").append(query.charAt(i)).append("</span>");
                midOut.append("<span%%class='alignmentMatch'>").append(mid.charAt(i)).append("</span>");
                subjOut.append("<span%%class='alignmentMatch'>").append(subj.charAt(i)).append("</span>");
            }
            else if (ch == '+') {
                queryOut.append("<span%%class='alignmentSimilar'>").append(query.charAt(i)).append("</span>");
                midOut.append("<span%%class='alignmentSimilar'>").append(mid.charAt(i)).append("</span>");
                subjOut.append("<span%%class='alignmentSimilar'>").append(subj.charAt(i)).append("</span>");
            }
            else if (ch == '*') {
                queryOut.append("<span%%class='alignmentStopCodon'>").append(query.charAt(i)).append("</span>");
                midOut.append("<span%%class='alignmentStopCodon'>").append(mid.charAt(i)).append("</span>");
                subjOut.append("<span%%class='alignmentStopCodon'>").append(subj.charAt(i)).append("</span>");
            }
            else if (String.valueOf(ch).matches("[A-Z]")) {
                queryOut.append("<span%%class='alignmentMatch'>").append(query.charAt(i)).append("</span>");
                midOut.append("<span%%class='alignmentMatch'>").append(mid.charAt(i)).append("</span>");
                subjOut.append("<span%%class='alignmentMatch'>").append(subj.charAt(i)).append("</span>");
            }
            else {
                queryOut.append(query.charAt(i));
                midOut.append(mid.charAt(i));
                subjOut.append(subj.charAt(i));
            }
        }
        return new String[]{queryOut.toString(), midOut.toString(), subjOut.toString()};
    }

    public String getHitStatistics() throws Exception {

        Integer qFrame = getQueryFrame();
        Integer sFrame = getSubjectFrame();

        //  Determine how to display output based on program used
        String framestrandText = null;
        if (programUsed.equals("blastn")) {
            framestrandText = " Strand = " + (qFrame == -1 ? "Minus" : "Plus") + " / " + (sFrame == -1 ? "Minus" : "Plus");
        }
        else if (programUsed.equals("blastp")) {
            // Should be obvious, so don't report, it's always +1/+1
        }
        else if (programUsed.equals("blastx")) {
            framestrandText = " Frame = " + (qFrame == 1 ? "+" + qFrame.toString() : qFrame.toString());
        }
        else if (programUsed.equals("tblastn")) {
            framestrandText = " Frame = " + (sFrame == 1 ? "+" + sFrame.toString() : sFrame.toString());
        }
        else if (programUsed.equals("tblastx")) {
            framestrandText = " Frame = " + (qFrame == 1 ? "+" + qFrame.toString() : "-" + qFrame.toString()) + " / " + (sFrame == 1 ? "+" + sFrame.toString() : "-" + sFrame.toString());
        }
        else if (programUsed.equals("megablast")) {
            framestrandText = " Strand = " + (qFrame == -1 ? "Minus" : "Plus") + " / " + (sFrame == -1 ? "Minus" : "Plus");
        }
        else {
            throw new Exception("Program type was not set.  Cannot describe Strandidness.");
        }

        // Put output string together
        String output = "";

        output += " Score = " + Math.round(getBitScore().doubleValue() * 10) / 10.0 + " bits (" + getHspScore().doubleValue() + "), Expect = " + getExpectScore() + "\n";
        output += " Identities = " + getNumberIdentical().toString() + "/" + getLengthAlignment().toString() +
                " (" + (100 * getNumberIdentical() / getLengthAlignment()) + "%), ";
        output += "Positives = " + (getNumberIdentical() + getNumberSimilar()) + "/" + getLengthAlignment().toString() +
                " (" + ((100 * getNumberIdentical() + getNumberSimilar()) / getLengthAlignment()) + "%), ";
        output += "Gaps = " + getQueryGaps().toString() + "/" + getLengthAlignment().toString() +
                " (" + (100 * getQueryGaps() / getLengthAlignment()) + "%)\n";
        output += framestrandText;

        return output;

    }

    public int compareTo(Object obj) {

        BlastHit bh = (BlastHit) obj;

        int result = 0;
        if (this.getQueryAcc() != null && bh.getQueryAcc() != null) {
            result = this.getQueryAcc().compareTo(bh.getQueryAcc());
        }
        else if (this.getQueryAcc() != null) {
            result = -1;
        }
        else if (bh.getQueryAcc() != null) {
            result = 1;
        }
        if (result == 0) {
            result = this.getExpectScore().compareTo(bh.getExpectScore());
            if (result == 0) {
                result = this.getBitScore().compareTo(bh.getBitScore());
                if (result == 0) {
                    result = this.getLengthAlignment().compareTo(bh.getLengthAlignment());
                    if (result == 0) {
                        result = this.getNumberSimilar().compareTo(bh.getNumberSimilar());
                        if (result == 0) {
                            result = this.getQueryBegin().compareTo(bh.getQueryBegin());
                            if (result == 0) {
                                result = this.getSubjectBegin().compareTo(bh.getSubjectBegin());
                                if (result == 0) {
                                    result = this.getQueryEnd().compareTo(bh.getQueryEnd());
                                    if (result == 0) {
                                        result = this.getSubjectEnd().compareTo(bh.getSubjectEnd());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    public String toString() {
        String s = "";

        s += "\nalignment_id: " + this.getBlastHitId() + "\n";
        s += "\nBlast Hit (" + programUsed + ") \n";
        s += "\tbitScore: " + bitScore + " hspScore: " + hspScore + " expectScore: " + expectScore + "\n";
        s += "\tcomment: \"" + comment + "\"\n";
        s += "\tlengthAlignment: " + lengthAlignment + " numIdentical: " + numberIdentical + " numSimilar: " + numberSimilar + "\n";
        s += "\tentropy: " + entropy + "\n\n";

        s += "\t\tSubject\tQuery\n";
        s += "\tBegin: " + "\t" + getSubjectBegin() + "\t" + getQueryBegin() + "\n";
        s += "\tEnd: " + "\t" + getSubjectEnd() + "\t" + getQueryEnd() + "\n";
        s += "\tOrientation: " + "\t" + getSubjectOrientation() + "\t" + getQueryOrientation() + "\n";
        s += "\tLength: " + "\t" + subjectLength + "\t" + queryLength + "\n";
        s += "\tGaps: " + "\t" + subjectGaps + "\t" + queryGaps + "\n";
        s += "\tGap Runs: " + "\t" + subjectGapRuns + "\t" + queryGapRuns + "\n";
        s += "\tStops: " + "\t" + subjectStops + "\t" + queryStops + "\n";
        s += "\tUnalignable: " + "\t" + subjectNumberUnalignable + "\t" + queryNumberUnalignable + "\n";
        s += "\tFrame: " + "\t" + subjectFrame + "\t" + queryFrame + "\n\n";

        // Alignment strings
        s += "Subject: " + subjectAlignString + "\n";
        s += "Midline: " + midline + "\n";
        s += "Query  : " + queryAlignString + "\n";

        return s;
    }


//    public static void main(String[] args) {
//        BlastHit bh = new BlastHit();
//        bh.setQueryAlignString(
//                "NSTVSLTTKNMEVSVAKTTKAEIPILRMNFKQELNGNT-----KSKPTVSSSMEFKYDFNSSMLYSTAKGAVDHKLSLESLTSYFSIESSTKG------DVKGSVLSREYSGTIASEANTYLNSKSTRSSVKLQGTSKIDDIWNLEVKE");
//        bh.setSubjectAlignString(
//                "NTTTSNTQQNATVAVAGDGSF---VVTWESQNQDNGTTYGIYGQRYDALGVSQGSNFLVNQTVAGDQQYANVDSDSAGNFVVTWTSSDGNQDGIWARRFDSSGSALGDEF------QVNTYTTGNQSRSHVDMNESGEFIITWNSESQD");
//        bh.setMidline(
//                "N+T S T +N  V+VA        ++    + + NG T     +    +  S    +  N ++        VD   +   + ++ S + +  G      D  GS L  E+      + NTY     +RS V +  + +    WN E ++");
//        bh.setQueryBegin(new Integer(07));
//        bh.setQueryEnd(new Integer(3545));
//        bh.setSubjectBegin(new Integer(1983));
//        bh.setSubjectEnd(new Integer(2403));
//        bh.setSubjectOrientation(new Integer(0));
//        bh.setQueryOrientation(new Integer(0));
//
//        bh.setQueryFrame(new Integer(1));
//        bh.setSubjectFrame(new Integer(1));
//
//        bh.setProgramUsed("tblastn");
//
//        bh.setBitScore(new Float("25.8"));
//        bh.setHspScore(new Float(55));
//        bh.setExpectScore(new Double("1e-20"));
//        bh.setNumberIdentical(new Integer(31));
//        bh.setNumberSimilar(new Integer(26));
//        bh.setQueryGaps(new Integer(11));
//        bh.setSubjectGaps(new Integer(9));
//        bh.setLengthAlignment(new Integer(149));
//
//
//        BlastHit bh2 = new BlastHit();
//        bh.setQueryAlignString(
//                "NSTVSLTTKNMEVSVAKTTKAEIPILRMNFKQELNGNT-----KSKPTVSSSMEFKYDFNSSMLYSTAKGAVDHKLSLESLTSYFSIESSTKG------DVKGSVLSREYSGTIASEANTYLNSKSTRSSVKLQGTSKIDDIWNLEVKE");
//        bh.setSubjectAlignString(
//                "NTTTSNTQQNATVAVAGDGSF---VVTWESQNQDNGTTYGIYGQRYDALGVSQGSNFLVNQTVAGDQQYANVDSDSAGNFVVTWTSSDGNQDGIWARRFDSSGSALGDEF------QVNTYTTGNQSRSHVDMNESGEFIITWNSESQD");
//        bh.setMidline(
//                "N+T S T +N  V+VA        ++    + + NG T     +    +  S    +  N ++        VD   +   + ++ S + +  G      D  GS L  E+      + NTY     +RS V +  + +    WN E ++");
//        bh.setQueryBegin(new Integer(07));
//        bh.setQueryEnd(new Integer(3545));
//        bh.setSubjectBegin(new Integer(1983));
//        bh.setSubjectEnd(new Integer(2403));
//        bh.setSubjectOrientation(BlastHit.ALGN_ORI_REVERSE);
//        bh.setQueryOrientation(BlastHit.ALGN_ORI_REVERSE);
//
//        bh.setQueryFrame(new Integer(1));
//        bh.setSubjectFrame(new Integer(1));
//
//        bh.setProgramUsed("tblastn");
//
//        bh.setBitScore(new Float("25.8"));
//        bh.setHspScore(new Float(55));
//        bh.setExpectScore(new Double("1e-20"));
//        bh.setNumberIdentical(new Integer(31));
//        bh.setNumberSimilar(new Integer(26));
//        bh.setQueryGaps(new Integer(11));
//        bh.setSubjectGaps(new Integer(9));
//        bh.setLengthAlignment(new Integer(149));
//
//        try {
//            System.out.println(bh.getHitStatistics());
//            System.out.println(bh.getPairWiseAlignment(new Integer(80)));
//        }
//        catch (Exception e) {
//        }
//
//    }

    public Long getBlastHitId() {
        return blastHitId;
    }

    public void setBlastHitId(Long blastHitId) {
        this.blastHitId = blastHitId;
    }

    public String getProgramUsed() {
        return programUsed;
    }

    public void setProgramUsed(String programUsed) {
        this.programUsed = programUsed;
    }

    public Float getBitScore() {
        return bitScore;
    }

    public void setBitScore(Float bitScore) {
        this.bitScore = bitScore;
    }

    public Float getHspScore() {
        return hspScore;
    }

    public void setHspScore(Float hspScore) {
        this.hspScore = hspScore;
    }

    public Double getExpectScore() {
        return expectScore;
    }

    public void setExpectScore(Double expectScore) {
        this.expectScore = expectScore;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getLengthAlignment() {
        return lengthAlignment;
    }

    public void setLengthAlignment(Integer lengthAlignment) {
        this.lengthAlignment = lengthAlignment;
    }

    public Float getEntropy() {
        return entropy;
    }

    public void setEntropy(Float entropy) {
        this.entropy = entropy;
    }

    public Integer getNumberIdentical() {
        return numberIdentical;
    }

    public void setNumberIdentical(Integer numberIdentical) {
        this.numberIdentical = numberIdentical;
    }

    public Integer getNumberSimilar() {
        return numberSimilar;
    }

    public void setNumberSimilar(Integer numberSimilar) {
        this.numberSimilar = numberSimilar;
    }

    public BaseSequenceEntity getSubjectEntity() {
        return subjectEntity;
    }

    public void setSubjectEntity(BaseSequenceEntity subjectEntity) {
        this.subjectEntity = subjectEntity;
    }

    public Integer getSubjectBegin() {
        return subjectBegin;
    }

    public void setSubjectBegin(Integer subjectBegin) {
        this.subjectBegin = subjectBegin;
    }

    public Integer getSubjectEnd() {
        return subjectEnd;
    }

    public void setSubjectEnd(Integer subjectEnd) {
        this.subjectEnd = subjectEnd;
    }

    public Integer getSubjectOrientation() {
        return subjectOrientation;
    }

    public void setSubjectOrientation(Integer subjectOrientation) {
        this.subjectOrientation = subjectOrientation;
    }

    public BaseSequenceEntity getQueryEntity() {
        return queryEntity;
    }

    public void setQueryEntity(BaseSequenceEntity queryEntity) {
        this.queryEntity = queryEntity;
    }

    public Integer getQueryBegin() {
        return queryBegin;
    }

    public void setQueryBegin(Integer queryBegin) {
        this.queryBegin = queryBegin;
    }

    public Integer getQueryEnd() {
        return queryEnd;
    }

    public void setQueryEnd(Integer queryEnd) {
        this.queryEnd = queryEnd;
    }

    public Integer getQueryOrientation() {
        return queryOrientation;
    }

    public void setQueryOrientation(Integer queryOrientation) {
        this.queryOrientation = queryOrientation;
    }

    public Node getResultNode() {
        return resultNode;
    }

    public void setResultNode(Node resultNode) {
        this.resultNode = resultNode;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public String getSubjectAcc() {
        return subjectAcc;
    }

    public void setSubjectAcc(String subjectAcc) {
        this.subjectAcc = subjectAcc;
    }

    public Integer getSubjectLength() {
        return subjectLength;
    }

    public void setSubjectLength(Integer subjectLength) {
        this.subjectLength = subjectLength;
    }

    public Integer getSubjectGaps() {
        return subjectGaps;
    }

    public void setSubjectGaps(Integer subjectGaps) {
        this.subjectGaps = subjectGaps;
    }

    public Integer getSubjectGapRuns() {
        return subjectGapRuns;
    }

    public void setSubjectGapRuns(Integer subjectGapRuns) {
        this.subjectGapRuns = subjectGapRuns;
    }

    public Integer getSubjectStops() {
        return subjectStops;
    }

    public void setSubjectStops(Integer subjectStops) {
        this.subjectStops = subjectStops;
    }

    public Integer getSubjectNumberUnalignable() {
        return subjectNumberUnalignable;
    }

    public void setSubjectNumberUnalignable(Integer subjectNumberUnalignable) {
        this.subjectNumberUnalignable = subjectNumberUnalignable;
    }

    public Integer getSubjectFrame() {
        return subjectFrame;
    }

    public void setSubjectFrame(Integer subjectFrame) {
        this.subjectFrame = subjectFrame;
    }

    public Long getQueryNodeId() {
        return queryNodeId;
    }

    public void setQueryNodeId(Long queryNodeId) {
        this.queryNodeId = queryNodeId;
    }

    public String getQueryAcc() {
        return queryAcc;
    }

    public void setQueryAcc(String queryAcc) {
        this.queryAcc = queryAcc;
    }

    public Integer getQueryLength() {
        return queryLength;
    }

    public void setQueryLength(Integer queryLength) {
        this.queryLength = queryLength;
    }

    public Integer getQueryGaps() {
        return queryGaps;
    }

    public void setQueryGaps(Integer queryGaps) {
        this.queryGaps = queryGaps;
    }

    public Integer getQueryGapRuns() {
        return queryGapRuns;
    }

    public void setQueryGapRuns(Integer queryGapRuns) {
        this.queryGapRuns = queryGapRuns;
    }

    public Integer getQueryStops() {
        return queryStops;
    }

    public void setQueryStops(Integer queryStops) {
        this.queryStops = queryStops;
    }

    public Integer getQueryNumberUnalignable() {
        return queryNumberUnalignable;
    }

    public void setQueryNumberUnalignable(Integer queryNumberUnalignable) {
        this.queryNumberUnalignable = queryNumberUnalignable;
    }

    public Integer getQueryFrame() {
        return queryFrame;
    }

    public void setQueryFrame(Integer queryFrame) {
        this.queryFrame = queryFrame;
    }

    public String getSubjectAlignString() {
        return subjectAlignString;
    }

    public void setSubjectAlignString(String subjectAlignString) {
        this.subjectAlignString = subjectAlignString;
    }

    public String getMidline() {
        return midline;
    }

    public void setMidline(String midline) {
        this.midline = midline;
    }

    public String getQueryAlignString() {
        return queryAlignString;
    }

    public void setQueryAlignString(String queryAlignString) {
        this.queryAlignString = queryAlignString;
    }

    public String getBlastVersion() {
        return blastVersion;
    }

    public void setBlastVersion(String blastVersion) {
        this.blastVersion = blastVersion;
    }
}
