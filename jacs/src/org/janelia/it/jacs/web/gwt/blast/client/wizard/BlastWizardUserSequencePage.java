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

import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.genomics.SequenceType;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBoxActionLinkUtils;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.UploadUserSequencePanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.ButtonSet;
import org.janelia.it.jacs.web.gwt.common.client.ui.DoubleClickSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.HelpActionLink;
import org.janelia.it.jacs.web.gwt.common.client.util.BlastData;
import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardController;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Nov 6, 2006
 * Time: 4:29:47 PM
 */
public class BlastWizardUserSequencePage extends BlastWizardPage {
    public static final String HISTORY_TOKEN = "BlastWizardUserSequencePage";
    public static final String BLAST_SEQUENCE_HELP_LINK_PROP = "BlastSequence.HelpURL";
    private static final String UPLOAD_MESSAGE = "File upload successful - click Next to continue.";

    private TitledBox _mainPanel;
    private UploadUserSequencePanel _sequencePanel;

    public BlastWizardUserSequencePage(BlastData blastData, WizardController controller) {
        super(blastData, controller);
        init();
    }

    private void init() {
        _mainPanel = new TitledBox("Specify Query Sequences");
        _mainPanel.removeActionLinks();
        TitledBoxActionLinkUtils.addHelpActionLink(_mainPanel, new HelpActionLink("help"), BLAST_SEQUENCE_HELP_LINK_PROP);

        //To change body of created methods use File | Settings | File Templates.
        _sequencePanel = new UploadUserSequencePanel(new SequenceSelectionListener(), new SequenceDoubleClickSelectedListener(),
                new String[]{"5", "10", "20"}, getData(), SequenceType.NOT_SPECIFIED, 5);
        _sequencePanel.setUploadMessage(UPLOAD_MESSAGE);
        _sequencePanel.addUpperRightPanel(getUpperButtonPanel());
        _mainPanel.add(_sequencePanel);
    }

    private Widget getUpperButtonPanel() {
        RoundedButton lowerBackButton = getController().getButtonManager().getBackButton();
        RoundedButton lowerNextButton = getController().getButtonManager().getNextButton();
        RoundedButton upperBackButton = new RoundedButton("Back", lowerBackButton.getRemoteClickListener());
        RoundedButton upperNextButton = new RoundedButton("Next", lowerNextButton.getRemoteClickListener());
        lowerBackButton.addLinkedButton(upperBackButton);
        lowerNextButton.addLinkedButton(upperNextButton);

        ButtonSet upperButtonSet = new ButtonSet(new RoundedButton[]{upperBackButton, upperNextButton});
        upperButtonSet.setStyleName("buttonSetTop");

        HTMLPanel upperButtonSetPanel = new HTMLPanel("<span id='buttonSetTop'></span>");
        upperButtonSetPanel.add(upperButtonSet, "buttonSetTop");

        return upperButtonSetPanel;
    }

    public class SequenceSelectionListener implements SelectionListener {
        public void onSelect(String value) {
            // Set the next button true if there's some activity in the sequence selection panel.  We can't check
            // the validity of pasted text yet because this callback is called on every keystroke, so you get a
            // nasty loop if we invalidate the text and remove focus on every keystroke.  The typed/pasted sequence
            // will be validated when the Next button is pressed.
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

    public Widget getMainPanel() {
        return _mainPanel;
    }

    public String getPageTitle() {
        return Constants.JOBS_WIZARD_QUERY_SEQ_LABEL;
    }

    protected void preProcess(Integer priorPageNumber) {
    }

    protected void setupButtons() {
        super.setupButtons();

        // Enable next button if page is fine
        getButtonManager().setBackButtonEnabled(false);
        getButtonManager().setNextButtonEnabled(_sequencePanel.validateAndPersistSequenceSelection());
    }

    public String getPageToken() {
        return HISTORY_TOKEN;
    }

    protected boolean isProgressionValid() {
        if (_sequencePanel.isPastedSequence())
            return _sequencePanel.validateAndPersistSequenceSelection(); // will show error message if invalid
        else
            return true;
    }
}
