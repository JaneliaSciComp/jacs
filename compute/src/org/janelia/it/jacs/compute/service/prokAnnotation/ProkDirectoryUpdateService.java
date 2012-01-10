
package org.janelia.it.jacs.compute.service.prokAnnotation;

import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.prokAnnotation.ProkAnnotationLocalDirectoryImportTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.prokAnnotation.ProkAnnotationResultFileNode;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Nov 2, 2009
 * Time: 11:01:30 AM
 */
public class ProkDirectoryUpdateService implements IService {
    @Override
    public void execute(IProcessData processData) throws ServiceException {
        try {
            // Step 1 - Get the list of output nodes from the db and populate collection
            ComputeBeanLocal computeBean = EJBFactory.getLocalComputeBean();
            List<Node> resultNodes = computeBean.getNodesByClassAndUser(ProkAnnotationResultFileNode.class.getSimpleName(),
                    User.SYSTEM_USER_LOGIN);
            TreeMap<String, ProkAnnotationResultFileNode> outputMap = new TreeMap<String, ProkAnnotationResultFileNode>();
            for (Node resultNode : resultNodes) {
                outputMap.put(resultNode.getName(), (ProkAnnotationResultFileNode) resultNode);
            }

            // Step 2 - Get the file list from the filestore
            File annotationDir = new File(SystemConfigurationProperties.getString("ProkAnnotation.BaseDir"));

            // Step 3 - Compare if the filestore dirs have been backed by official output nodes
            File[] genomeDirs = annotationDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return (new File(dir.getAbsolutePath() + File.separator + name)).isDirectory();
                }
            });
            int additionCounter = 0;
            for (File genomeDir : genomeDirs) {
                if (!outputMap.containsKey(genomeDir.getName())) {
                    additionCounter++;
                    System.out.println("(" + additionCounter + ") Adding new output node for " + genomeDir.getName());
                    ProkAnnotationLocalDirectoryImportTask tmpTask = new ProkAnnotationLocalDirectoryImportTask(genomeDir.getAbsolutePath(),
                            genomeDir.getName(), new HashSet<Node>(), User.SYSTEM_USER_LOGIN, new ArrayList<Event>(), new HashSet<TaskParameter>());
                    computeBean.saveOrUpdateTask(tmpTask);
                    computeBean.submitJob("ProkAnnotationLocalDirectoryImport", tmpTask.getObjectId());
                }
            }
        }
        catch (DaoException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
