/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/22/11
 * Time: 6:10 PM
 */
package org.janelia.it.jacs.model.ontology;

import java.math.BigDecimal;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Interval extends OntologyTermType {

    private Number lowerBound;
    private Number upperBound;

    public void init(String lowerBoundStr, String upperBoundStr) {
        lowerBound = new BigDecimal(lowerBoundStr);
        upperBound = new BigDecimal(upperBoundStr);
    }

    public void init(Entity entity) {
        String lowerBoundStr = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE_INTERVAL_LOWER);
        String upperBoundStr = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE_INTERVAL_UPPER);
        init(lowerBoundStr, upperBoundStr);
    }

    public boolean allowsChildren() {
        return false;
    }

    public String getName() {
    	return "Interval";
    }
    
    public Number getLowerBound() {
        return lowerBound;
    }

    public Number getUpperBound() {
        return upperBound;
    }

}
