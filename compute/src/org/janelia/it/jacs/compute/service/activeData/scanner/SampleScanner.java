/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.service.activeData.scanner;

import java.io.File;
import java.util.List;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.service.activeData.ActiveDataScan;
import org.janelia.it.jacs.compute.service.activeData.VisitorFactory;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 *
 * @author murphys
 */
public class SampleScanner extends EntityScanner {

    static final Logger logger = Logger.getLogger(SampleScanner.class);
    
    public SampleScanner() {
        super();
    }

    public SampleScanner(List<VisitorFactory> visitorFactoryList) {
        super(visitorFactoryList);
    }

    @Override
    public long[] generateIdList(Object dataResource) throws Exception {
        return generateIdListByEntityType(dataResource, EntityConstants.TYPE_SAMPLE);
    }

    @Override
    public void preEpoch(ActiveDataScan scan) throws Exception {
        File preTestFile = new File(scan.getScanDirectory() + "/" + "preTestFile.txt");
        logger.info("preEpoch() - preTestFile.txt path="+preTestFile.getAbsolutePath());
        preTestFile.createNewFile();
    }

    @Override
    public void postEpoch(ActiveDataScan scan) throws Exception {
        File postTestFile = new File(scan.getScanDirectory() + "/" + "postTestFile.txt");
        logger.info("postEpoch() - postTestFile.txt path="+postTestFile.getAbsolutePath());
        postTestFile.createNewFile();
    }

}
