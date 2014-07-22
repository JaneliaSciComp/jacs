/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.service.geometricSearch;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.janelia.it.jacs.compute.service.activeData.ActiveTestVisitor;
import org.janelia.it.jacs.compute.service.activeData.EntityScanner;
import org.janelia.it.jacs.compute.service.activeData.SampleScanner;
import org.janelia.it.jacs.compute.service.activeData.VisitorFactory;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.geometricSearch.GeometricIndexTask;

/**
 *
 * @author murphys
 */
public class GeometricIndexService extends AbstractEntityService {

    @Override
    protected void execute() throws Exception {
        GeometricIndexTask indexTask=(GeometricIndexTask)task;
        computeBean.saveEvent(indexTask.getObjectId(), Event.RUNNING_EVENT, "Running", new Date());
        List<VisitorFactory> geometricIndexVisitors=new ArrayList<>();
        Map<String,Object> parameterMap=new HashMap<>();
        VisitorFactory testFactory=new VisitorFactory(parameterMap, ActiveTestVisitor.class);
        geometricIndexVisitors.add(testFactory);
        SampleScanner sampleScanner=new SampleScanner(geometricIndexVisitors);
        sampleScanner.setRemoveAfterEpoch(true);
        EntityScanner.addActiveDataScanner(sampleScanner);
    }
    
}
