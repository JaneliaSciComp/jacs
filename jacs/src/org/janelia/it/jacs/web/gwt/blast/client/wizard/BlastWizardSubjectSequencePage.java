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

package org.janelia.it.jacs.web.gwt.blast.client.wizard;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.genomics.SequenceType;
import org.janelia.it.jacs.web.gwt.blast.client.panel.BlastableNodePanel;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBoxActionLinkUtils;
import org.janelia.it.jacs.web.gwt.common.client.ui.DoubleClickSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.HelpActionLink;
import org.janelia.it.jacs.web.gwt.common.client.util.BlastData;
import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardController;

import java.util.HashMap;

/**
 * @author mpress
 */
public class BlastWizardSubjectSequencePage extends BlastWizardPage {
    public static final String BLAST_SUBJECT_SEQUENCE_HELP_LINK_PROP = "BlastSubject.HelpURL";
    public static final String HISTORY_TOKEN = "BlastWizardSubjectSequencePage";
    private TitledBox _mainPanel;

    public BlastWizardSubjectSequencePage(BlastData blastData, WizardController controller) {
        super(blastData, controller);
        init();
    }

    private void init() {
        _mainPanel = new TitledBox("Select a Reference Dataset");
        _mainPanel.removeActionLinks();
        TitledBoxActionLinkUtils.addHelpActionLink(_mainPanel, new HelpActionLink("help"), BLAST_SUBJECT_SEQUENCE_HELP_LINK_PROP);

        BlastableNodePanel blastableNodePanel = new BlastableNodePanel(SequenceType.NOT_SPECIFIED, new SequenceSelectionListener(), new SequenceDoubleClickSelectedListener(), getData());
        blastableNodePanel.realize();

        _mainPanel.add(blastableNodePanel);
        _mainPanel.add(new HTML("&nbsp;"));
    }

    public class SequenceSelectionListener implements SelectionListener {
        public void onSelect(String value) {
            getButtonManager().getNextButton().setEnabled(true);
        }

        public void onUnSelect(String value) {
            getButtonManager().getNextButton().setEnabled(false);
        }
    }

    public class SequenceDoubleClickSelectedListener implements DoubleClickSelectionListener {
        public void onSelect(String value) {
            getButtonManager().getNextButton().execute();
        }
    }

    protected void preProcess(Integer priorPageNumber) {
    }

    public Widget getMainPanel() {
        return _mainPanel;
    }

    public String getPageTitle() {
        return Constants.JOBS_WIZARD_SUBJECT_SEQ_LABEL;
    }

    public String getPageToken() {
        return HISTORY_TOKEN;
    }

    /**
     * Checks if the next button should be enabled based on user selections
     */
    public void setupButtons() {
        super.setupButtons();

        // See if suitable data already has been selected
        HashMap selections = getData().getSubjectSequenceDataNodeMap();
        if (null != selections && selections.size() > 0) {
            getButtonManager().setNextButtonEnabled(true);
        }
        else {
            getButtonManager().setNextButtonEnabled(false);
        }
    }
}
