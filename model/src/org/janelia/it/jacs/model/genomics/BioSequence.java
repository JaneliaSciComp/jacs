
package org.janelia.it.jacs.model.genomics;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: jhoover
 * Date: Dec 7, 2006
 * Time: 9:20:38 AM
 */
abstract public class BioSequence implements Serializable, IsSerializable {

    public static int FORWARD_ORIENTATION = 1;
    public static int REVERSE_ORIENTATION = -1;
    /*
     * fields
     */
    private Long sequenceId;
    private SequenceType sequenceType;
    private String sequence = "";
    private Integer sourceId = 0;

    public BioSequence() {
    }

    protected BioSequence(SequenceType sequenceType) {
        this.sequenceType = sequenceType;
    }

    /*
    * getters
    */
    public Long getSequenceId() {
        return sequenceId;
    }

    public SequenceType getSequenceType() {
        return sequenceType;
    }

    public int getLength() {
        String s = getSequence();
        return s != null ? s.length() : 0;
    }

    public String toString() {
        return "Sequence{" +
                "sequenceId=" + getSequenceId() +
                ", sequenceType=" + getSequenceType().getName() +
                ", sequence='" + getSequence() + '\'' +
                '}';
    }

    /*
    * sequence manipulation
    */
    public BioSequence subSequence(Integer begin, Integer end, Integer ori) {
        return subSequence(begin.intValue(), end.intValue(), ori.intValue());
    }

    public BioSequence subSequence(int begin, int end, int ori) {
        if (begin < 0 | begin > end)
            throw new SequenceException(
                    "Invalid subsequence request, improper range specification \""
                            .concat(Integer.toString(begin)).concat("-")
                            .concat(Integer.toString(end)).concat("\"."));
        else if (end > getLength())
            throw new SequenceException(
                    "Invalid subsequence request, range specification \""
                            .concat(Integer.toString(begin)).concat("-").concat(Integer.toString(end))
                            .concat("\" is out of bounds (0-").concat(Integer.toString(getLength())).concat(")."));
        else {
            BioSequence subseq;
            if (sequenceType == SequenceType.NA)
                subseq = new NASequence();
            else
                subseq = new AASequence();
            if (getSequence() != null) subseq.setSequence(getSequence().substring(begin, end));

            if (ori == FORWARD_ORIENTATION)
                return subseq;
            else if (ori == REVERSE_ORIENTATION)
                return subseq.reverse();
            else
                throw new SequenceException(
                        "Invalid subsequence request, unrecognized value specified for orientation: \""
                                .concat(Integer.toString(ori)).concat("\"."));
        }
    }


    public BioSequence reverse() {
        BioSequence revseq;
        if (sequenceType == SequenceType.NA)
            revseq = new NASequence();
        else
            revseq = new AASequence();
        revseq.setSequence(SeqUtil.reverse(getSequence()));
        return revseq;
    }

    public void setSequenceId(Long sequenceId) {
        this.sequenceId = sequenceId;
    }

    public void setSequenceType(SequenceType sequenceType) {
        this.sequenceType = sequenceType;
    }


    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public String getSequence() {
        return sequence;
    }

    public Integer getSourceId() {
        return sourceId;
    }

    public void setSourceId(Integer sourceId) {
        this.sourceId = sourceId;
    }
}
