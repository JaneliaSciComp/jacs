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

package org.janelia.it.jacs.web.gwt.detail.client.bse.peporf;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.RowIndex;
import org.janelia.it.jacs.web.gwt.detail.client.bse.SequenceDetailsTableBuilder;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 6, 2008
 * Time: 2:28:59 PM
 */
public abstract class PeporfEntityPanel extends HorizontalPanel {
    protected PeporfPanel parentPanel;
    protected SequenceDetailsTableBuilder tableBuilder;
    protected TitledBox detailBox;
    protected FlexTable dataTable;
    protected LoadingLabel loadingLabel;

    public PeporfEntityPanel(PeporfPanel parentPanel) {
        this.parentPanel = parentPanel;
    }

    public void initialize(String title, String style) {
        if (detailBox == null) {
            detailBox = new TitledBox(title, true);
        }
        detailBox.setStyleName(style);
        if (dataTable == null) {
            dataTable = new FlexTable();
        }
        detailBox.add(dataTable);
        add(detailBox);
        loadingLabel = new LoadingLabel("Loading " + title + " data ...", true);
        detailBox.add(loadingLabel);
    }

    public abstract void display();

    protected int getSequenceBegin() {
        return 0;
    }

    protected int getSequenceEnd() {
        return 0;
    }

    protected LoadingLabel getLoadingLabel() {
        return loadingLabel;
    }

    protected TitledBox getDetailBox() {
        return detailBox;
    }

    public PeporfPanel getParentPanel() {
        return parentPanel;
    }

    public SequenceDetailsTableBuilder getTableBuilder() {
        return tableBuilder;
    }

    public void setTableBuilder(SequenceDetailsTableBuilder tableBuilder) {
        this.tableBuilder = tableBuilder;
    }

    public void setDataTable(FlexTable dataTable) {
        this.dataTable = dataTable;
    }

    public void displayDetail(BaseSequenceEntity bse) {
        tableBuilder.setBaseEntity(bse);
        String type = bse.getEntityType().getName();
        RowIndex rowIndex = tableBuilder.populateCAMERAAccNoAsTargetLink(type + " details",
                tableBuilder.getBaseEntity().getCameraAcc(), type, null);
        if (rowIndex != null)
            tableBuilder.populateEntityDetails(rowIndex);
    }

    void displaySequence(boolean asLinkOnlyFlag) {
        loadingLabel.setVisible(false);
        tableBuilder.populateSequenceData(null, asLinkOnlyFlag);
    }

    BaseSequenceEntity getEntity() {
        return tableBuilder.getBaseEntity();
    }

}
