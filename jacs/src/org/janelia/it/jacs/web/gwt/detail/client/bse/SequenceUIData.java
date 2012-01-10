
package org.janelia.it.jacs.web.gwt.detail.client.bse;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.web.gwt.common.client.ui.SpanList;

/**
 * This class is used by ReadDetail page to display the sequence
 */
public class SequenceUIData implements IsSerializable {
    private String sequenceLength;
    private String sequenceType;
    private SpanList sequenceSpanList;

    public String getSequenceLength() {
        return sequenceLength;
    }

    public void setSequenceLength(String sequenceLength) {
        this.sequenceLength = sequenceLength;
    }

    public String getSequenceType() {
        return sequenceType;
    }

    public void setSequenceType(String sequenceType) {
        this.sequenceType = sequenceType;
    }

    public SpanList getSequenceSpanList() {
        return sequenceSpanList;
    }

    public void setSequenceSpanList(SpanList sequenceSpanList) {
        this.sequenceSpanList = sequenceSpanList;
    }
}
