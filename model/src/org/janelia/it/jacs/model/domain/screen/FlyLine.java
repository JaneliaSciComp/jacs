package org.janelia.it.jacs.model.domain.screen;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.support.MongoMapped;

@MongoMapped(collectionName = "flyLine")
public class FlyLine extends AbstractDomainObject {

    private Long representativeId;
    private Long balancedLineId;
    private Long originalLineId;
    private Integer robotId;
    private String splitPart;

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    public Long getRepresentativeId() {
        return representativeId;
    }

    public void setRepresentativeId(Long representativeId) {
        this.representativeId = representativeId;
    }

    public Long getBalancedLineId() {
        return balancedLineId;
    }

    public void setBalancedLineId(Long balancedLineId) {
        this.balancedLineId = balancedLineId;
    }

    public Long getOriginalLineId() {
        return originalLineId;
    }

    public void setOriginalLineId(Long originalLineId) {
        this.originalLineId = originalLineId;
    }

    public Integer getRobotId() {
        return robotId;
    }

    public void setRobotId(Integer robotId) {
        this.robotId = robotId;
    }

    public String getSplitPart() {
        return splitPart;
    }

    public void setSplitPart(String splitPart) {
        this.splitPart = splitPart;
    }

}
