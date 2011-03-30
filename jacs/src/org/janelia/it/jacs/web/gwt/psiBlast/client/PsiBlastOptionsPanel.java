/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

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
