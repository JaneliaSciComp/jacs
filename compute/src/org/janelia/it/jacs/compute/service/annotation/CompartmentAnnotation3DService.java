package org.janelia.it.jacs.compute.service.annotation;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.screen.FlyScreenSampleService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.annotation.CompartmentAnnotation3DTask;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.entity.NamedFileNode;
import org.janelia.it.jacs.model.user_data.entity.PatternAnnotationResultNode;
import org.janelia.it.jacs.model.user_data.entity.ScreenSampleResultNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 9/10/13
 * Time: 1:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class CompartmentAnnotation3DService implements IService {

    private static final Logger logger = Logger.getLogger(CompartmentAnnotation3DService.class);
    CompartmentAnnotation3DTask task;
    FileNode inputNode;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger.info("CompartmentAnnotation3DService execute() start");

            // Get the input node, which should be a file with a list of stacks to process
            task = (CompartmentAnnotation3DTask)ProcessDataHelper.getTask(processData);
            File inputFile=new File(task.getParameter(CompartmentAnnotation3DTask.PARAM_inputStackListPath));

            if (inputFile.exists()) {
                logger.info("Found input file="+inputFile.getAbsolutePath());
            } else {
                throw new Exception("Could not find input file at location="+inputFile.getAbsolutePath());
            }


            logger.info("CompartmentAnnotation3DService execute() end");
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

}
