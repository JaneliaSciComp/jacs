package org.janelia.it.jacs.model.domain.screen;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.support.MongoMapped;
import org.janelia.it.jacs.model.domain.support.SearchAttribute;
import org.janelia.it.jacs.model.domain.support.SearchTraversal;
import org.janelia.it.jacs.model.domain.support.SearchType;

/**
 * A fly line associated with screen data. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@MongoMapped(collectionName="flyLine",label="Fly Line")
@SearchType(key="flyLine",label="Fly Line")
public class FlyLine extends AbstractDomainObject {

    @SearchTraversal({})
    private Reference representativeScreenSample;
    
    @SearchTraversal({})
    private Reference balancedLine;
    
    @SearchTraversal({})
    private Reference originalLine;

    @SearchAttribute(key="robot_id_i",label="Robot Id")
    private Integer robotId;
    
    @SearchAttribute(key="split_part_s",label="Split Part",facet=true)
    private String splitPart;

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    
    public Integer getRobotId() {
        return robotId;
    }
    
	public Reference getRepresentativeScreenSample() {
		return representativeScreenSample;
	}

	public void setRepresentativeScreenSample(Reference representativeScreenSample) {
		this.representativeScreenSample = representativeScreenSample;
	}

	public Reference getBalancedLine() {
		return balancedLine;
	}

	public void setBalancedLine(Reference balancedLine) {
		this.balancedLine = balancedLine;
	}

	public Reference getOriginalLine() {
		return originalLine;
	}

	public void setOriginalLine(Reference originalLine) {
		this.originalLine = originalLine;
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
