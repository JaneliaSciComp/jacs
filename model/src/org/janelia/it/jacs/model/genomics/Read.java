
package org.janelia.it.jacs.model.genomics;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Read extends Nucleotide implements Serializable, IsSerializable {

    private String traceAcc;
    private String templateAcc;
    private String sequencingDirection;
    private Integer clearRangeBegin;
    private Integer clearRangeEnd;
    private Set<Read> mates = new HashSet<Read>(0);

    public Read() {
        super(EntityTypeGenomic.READ);
    }

    public String getTraceAcc() {
        return traceAcc;
    }

    public void setTraceAcc(String traceAcc) {
        this.traceAcc = traceAcc;
    }

    public String getTemplateAcc() {
        return templateAcc;
    }

    public void setTemplateAcc(String templateAcc) {
        this.templateAcc = templateAcc;
    }

    public String getSequencingDirection() {
        return sequencingDirection;
    }

    public void setSequencingDirection(String sequencingDirection) {
        this.sequencingDirection = sequencingDirection;
    }

    public Integer getClearRangeBegin() {
        return clearRangeBegin;
    }

    public void setClearRangeBegin(Integer clearRangeBegin) {
        this.clearRangeBegin = clearRangeBegin;
    }

    public Integer getClearRangeBegin_oneResCoords() {
        if (clearRangeBegin == null) {
            return null;
        }
        else {
            return clearRangeBegin + 1;
        }
    }

    public Integer getClearRangeEnd() {
        return clearRangeEnd;
    }

    public void setClearRangeEnd(Integer clearRangeEnd) {
        this.clearRangeEnd = clearRangeEnd;
    }

    public Integer getClearRangeEnd_oneResCoords() {
        return clearRangeEnd;
    }

    public Set<Read> getMates() {
        return mates;
    }

    public void setMates(Set<Read> mates) {
        this.mates = mates;
    }
}
