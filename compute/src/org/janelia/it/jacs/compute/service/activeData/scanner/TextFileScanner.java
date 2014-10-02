package org.janelia.it.jacs.compute.service.activeData.scanner;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.service.activeData.ActiveDataScan;
import org.janelia.it.jacs.compute.service.activeData.VisitorFactory;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by murphys on 10/1/14.
 */
public class TextFileScanner extends EntityScanner {

    static final Logger logger = Logger.getLogger(TextFileScanner.class);

    public TextFileScanner() {
        super();
    }

    public TextFileScanner(List<VisitorFactory> visitorFactoryList) {
        super(visitorFactoryList);
    }

    @Override
    public long[] generateIdList(Object dataResource) throws Exception {
        logger.info("Calling generateIdListByEntityTypeAndName...");
        long[] result = generateIdListByEntityTypeAndName(dataResource, EntityConstants.TYPE_TEXT_FILE, "AlignedFlyBrain.properties");
        logger.info("Done");
        return result;
    }

    @Override
    public void preEpoch(ActiveDataScan scan) throws Exception {
        File preTestFile = new File(scan.getScanDirectory() + "/" + "preTestFile.txt");
        logger.info("preEpoch() - preTestFile.txt path="+preTestFile.getAbsolutePath());
        preTestFile.createNewFile();
    }

    @Override
    public void postEpoch(ActiveDataScan scan) throws Exception {

        // Standard
        File postTestFile = new File(scan.getScanDirectory() + "/" + "postTestFile.txt");
        logger.info("postEpoch() - postTestFile.txt path="+postTestFile.getAbsolutePath());
        BufferedWriter bw=new BufferedWriter(new FileWriter(postTestFile));

        // Do event type count
        Map<String, Long> eventCountMap=getEventCountMap();
        for (String key : eventCountMap.keySet()) {
            Long count=eventCountMap.get(key);
            String fileLine="Key="+key+" Count="+count;
            logger.info(fileLine);
            bw.write(fileLine+"\n");
        }
        bw.close();
    }

}
