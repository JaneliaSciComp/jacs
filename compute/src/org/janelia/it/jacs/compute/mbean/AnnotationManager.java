
package org.janelia.it.jacs.compute.mbean;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashSet;

import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.colorSeparator.ColorSeparatorTask;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorTask;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jul 2, 2007
 * Time: 5:00:41 PM
 */
@Singleton
@Startup
@Remote(AnnotationManagerMBean.class)
public class AnnotationManager extends AbstractComponentMBean implements AnnotationManagerMBean {

    public AnnotationManager() {
        super("jacs");
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
        catch (Exception e) {
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
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void generateUserAnnotationReport(String username) {
        /**
         * Use this as the basis of the query
         *
         select ed.id, e2.name, e.name
         from entity e, entityData ed, entity e2
         where e.id = ed.parent_entity_id
         and ed.entity_att_id=1629239031722148011
         and e.id in (select entityData.parent_entity_id from entityData where user_id=1676846821353193561 and entityData.entity_att_id=1629239031722148011)
         and e2.id = ed.value order by e2.name desc, ed.id asc
         */
    }
}