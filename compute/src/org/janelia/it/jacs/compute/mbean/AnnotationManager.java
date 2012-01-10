
package org.janelia.it.jacs.compute.mbean;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.colorSeparator.ColorSeparatorTask;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorTask;
import org.janelia.it.jacs.model.user_data.Node;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jul 2, 2007
 * Time: 5:00:41 PM
 */
public class AnnotationManager implements AnnotationManagerMBean {

    private static final Logger LOGGER = Logger.getLogger(AnnotationManager.class);

    public AnnotationManager() {
    }

    public void testNeuronSep(String inputFilePath) {
        try {
            NeuronSeparatorTask neuTask = new NeuronSeparatorTask(new HashSet<Node>(), "saffordt", new ArrayList<Event>(),
                    new HashSet<TaskParameter>());
            neuTask.setJobName("Neuron Separator Test");
            neuTask.setParameter(NeuronSeparatorTask.PARAM_inputTifFilePath, inputFilePath);
            neuTask = (NeuronSeparatorTask)EJBFactory.getLocalComputeBean().saveOrUpdateTask(neuTask);
            EJBFactory.getLocalComputeBean().submitJob("NeuronSeparation", neuTask.getObjectId());
        }
        catch (DaoException e) {
            e.printStackTrace();
        }
    }

    public void testColorSep(String inputFileDirectory) {
        try {
            File tmpFile = new File(inputFileDirectory);
            File[] tmpFiles = tmpFile.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File file, String name) {
                    return (name.indexOf(".seg_")>0);
                }
            });
            StringBuffer sbuf = new StringBuffer();
            for (int i = 0; i < tmpFiles.length; i++) {
                File file = tmpFiles[i];
                sbuf.append(file.getAbsolutePath());
                if ((i+1)<tmpFiles.length) { sbuf.append(","); }
            }
            ColorSeparatorTask colorTask = new ColorSeparatorTask(new HashSet<Node>(), "saffordt", new ArrayList<Event>(),
                    new HashSet<TaskParameter>());
            colorTask.setJobName("Color Separator Test");
            colorTask.setParameter(ColorSeparatorTask.PARAM_inputFileList, sbuf.toString());
            colorTask = (ColorSeparatorTask)EJBFactory.getLocalComputeBean().saveOrUpdateTask(colorTask);
            EJBFactory.getLocalComputeBean().submitJob("ColorSeparation", colorTask.getObjectId());
        }
        catch (DaoException e) {
            e.printStackTrace();
        }
    }

}