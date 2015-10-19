
package org.janelia.it.jacs.web.gwt.psiBlast.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.psiBlast.PsiBlastTask;
import org.janelia.it.jacs.web.gwt.blast.client.panel.BlastOptionsPanel;
import org.janelia.it.jacs.web.gwt.common.client.util.BlastData;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Dec 18, 2008
 * Time: 10:06:34 AM
 */
public class PsiBlastOptionsPanel extends BlastOptionsPanel {

    public PsiBlastOptionsPanel(BlastData blastData) {
        super(blastData);
    }

    // Temporary work-through method
    public void updateBlastPrograms() {
        updateBlastPrograms("", "", null);
    }

    public void updateBlastPrograms(String querySequenceType, String subjectSequenceType, final AsyncCallback asyncCallback) {
        // Keep the list around a little while
        Task[] data = new Task[]{new PsiBlastTask()};
        _blastData.setTaskArray(data);

        // Update the list with the display name of these programs
        _logger.debug("Adding the programs to the list box");
        for (Task task : data)
            listBox.addItem(task.getDisplayName());

        // Set the correct initial value
        if (prePopulated()) {
            for (int i = 0; i < data.length; i++) {
                if (data[i].getDisplayName().equals(_blastData.getBlastTask().getDisplayName())) {
                    _logger.debug("Setting the selection to " + i);
                    listBox.setSelectedIndex(i);
                    break;
                }
            } // end of for
            _blastData.setBlastTask(data[listBox.getSelectedIndex()]);
        }
        else {
            _logger.debug("Setting the selection to 0");
            listBox.setSelectedIndex(0);
            _blastData.setBlastTask(data[0]);
        }
        _taskOptionsPanel.displayParams(_blastData.getBlastTask());
    }

}
