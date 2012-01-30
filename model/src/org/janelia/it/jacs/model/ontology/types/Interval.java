/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/22/11
 * Time: 6:10 PM
 */
package org.janelia.it.jacs.model.ontology.types;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

import java.math.BigDecimal;

/**
 * An precise interval on the real number line. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Interval extends OntologyElementType {

    private BigDecimal lowerBound;
    private BigDecimal upperBound;

    public void init(String lowerBoundStr, String upperBoundStr) {
        lowerBound = new BigDecimal(lowerBoundStr);
        upperBound = new BigDecimal(upperBoundStr);
        if (lowerBound.compareTo(upperBound) >= 0) {
        	throw new IllegalArgumentException("Lower bound must be less than upper bound");
        }
    }

    public void init(Entity entity) {
        String lowerBoundStr = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE_INTERVAL_LOWER);
        String upperBoundStr = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE_INTERVAL_UPPER);
        init(lowerBoundStr, upperBoundStr);
    }

    public boolean allowsChildren() {
        return true;
    }

    public String getName() {
    	return "Interval";
    }
    
    public BigDecimal getLowerBound() {
        return lowerBound;
    }

    public BigDecimal getUpperBound() {
        return upperBound;
    }

}
