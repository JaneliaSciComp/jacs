/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.service.activeData;

import java.util.List;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 *
 * @author murphys
 */
public class SampleScanner extends EntityScanner {

    static final Logger logger = Logger.getLogger(SampleScanner.class);

    public SampleScanner(List<VisitorFactory> visitorFactoryList) {
        super(visitorFactoryList);
    }

    @Override
    public long[] generateIdList(Object dataResource) {
        return generateIdListByEntityType(dataResource, EntityConstants.TYPE_SAMPLE);
    }

}
