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

package org.janelia.it.jacs.web.gwt.rnaSeq.client.panel;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.janelia.it.jacs.model.common.UserDataNodeVO;
import org.janelia.it.jacs.model.tasks.rnaSeq.RnaSeqPipelineTask;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.UserNodeManagementPanel;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jul 20, 2010
 * Time: 10:43:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class RnaSeqTaskConfigurationPanel extends Composite implements UserNodeManagementPanel.NodeSelectedAction {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.rnaSeq.client.panel.RnaSeqTaskConfigurationPanel");

    RnaSeqPipelineTask rnaSeqPipelineTask;
    private VerticalPanel mainPanel;
    private RnaSeqPipelineTaskOptionsPanel rnaSeqPipelineTaskOptionsPanel;

    public RnaSeqTaskConfigurationPanel(RnaSeqPipelineTask rnaSeqPipelineTask) {
        this.rnaSeqPipelineTask=rnaSeqPipelineTask;
        init();
    }

    private void init() {
        _logger.debug("RnaSeqTaskConfigurationPanel: init() start");
        mainPanel = new VerticalPanel();
        rebuildTaskOptionsPanel();
        initWidget(mainPanel);
    }

    private void rebuildTaskOptionsPanel() {
        if (rnaSeqPipelineTaskOptionsPanel!=null) {
            mainPanel.remove(rnaSeqPipelineTaskOptionsPanel);
        }
        rnaSeqPipelineTaskOptionsPanel = new RnaSeqPipelineTaskOptionsPanel();
        rnaSeqPipelineTaskOptionsPanel.setStyleName("AdvancedBlastProgramOptionsPanel");
        rnaSeqPipelineTaskOptionsPanel.displayParams(rnaSeqPipelineTask);
        mainPanel.add(rnaSeqPipelineTaskOptionsPanel);
    }

    public void nodeSelected(UserDataNodeVO node) {
        _logger.info("RnaSeqTaskConfigurationPanel: node "+node.getDataType()+" selected");
        if (node.getDataType().equals("org.janelia.it.jacs.model.user_data.FastqDirectoryNode")) {
            rnaSeqPipelineTask.setParameter(RnaSeqPipelineTask.PARAM_input_reads_fastQ_node_id, node.getDatabaseObjectId());
        } else if (node.getDataType().equals("org.janelia.it.jacs.model.user_data.rnaSeq.RnaSeqReferenceGenomeNode")) {
            rnaSeqPipelineTask.setParameter(RnaSeqPipelineTask.PARAM_input_refgenome_fasta_node_id, node.getDatabaseObjectId());
        }
        rebuildTaskOptionsPanel();
    }

}
