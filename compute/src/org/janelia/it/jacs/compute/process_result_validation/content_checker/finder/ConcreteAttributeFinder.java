package org.janelia.it.jacs.compute.process_result_validation.content_checker.finder;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.process_result_validation.content_checker.FinderException;
import org.janelia.it.jacs.compute.process_result_validation.content_checker.validatable.AttributeFinder;
import org.janelia.it.jacs.compute.process_result_validation.content_checker.validatable.EntityFinder;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Use proprietary database schema to find the attribute of the type given.
 * Created by fosterl on 6/19/14.
 */
public class ConcreteAttributeFinder implements AttributeFinder {

    private Logger logger = Logger.getLogger(ConcreteAttributeFinder.class);
    //private AnnotationDAO annotationDAO;

    public ConcreteAttributeFinder() {
        //annotationDAO = new AnnotationDAO( logger );
    }

    @Override
    public String getAttribute(Entity parentEntity, String attributeName) throws FinderException {
        String rtnVal = null;
        try {
            // EntityAttribute attribute = annotationDAO.getEntityAttributeByName( attributeName );
            //  Q: need to "load" the child datas?
            rtnVal = parentEntity.getValueByAttributeName( attributeName );
        } catch ( Exception ex ) {
            String msg = "Failed to find attribute under " + parentEntity + " with name " + attributeName;
            logger.error( msg );
            throw new FinderException(msg, ex );
        }
        return rtnVal;
    }
}
