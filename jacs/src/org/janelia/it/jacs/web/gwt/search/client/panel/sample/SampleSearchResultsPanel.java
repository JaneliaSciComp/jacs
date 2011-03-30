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

package org.janelia.it.jacs.web.gwt.search.client.panel.sample;

import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.MultiValueSelectionListener;
import org.janelia.it.jacs.web.gwt.search.client.model.SearchResultsData;
import org.janelia.it.jacs.web.gwt.search.client.panel.CategorySearchResultTablePanel;
import org.janelia.it.jacs.web.gwt.search.client.panel.CategorySummarySearchPanel;
import org.janelia.it.jacs.web.gwt.search.client.panel.SearchResultsPanel;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 29, 2007
 * Time: 10:54:25 AM
 */
public class SampleSearchResultsPanel extends SearchResultsPanel {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.search.client.panel.sample.SampleSearchResultsPanel");

//    private SearchReadsTablePanel sampleReadsTablePanel;

    private class SampleSelectedListener implements MultiValueSelectionListener {
        public void onSelect(String[] values) {
//            sampleReadsTablePanel.populateSampleReads(/*sample acc*/ values[0], /*sample name*/ values[1]);
        }

        public void onUnSelect(String[] sampleAcc) {
            // nothing to do on unselect
        }
    }

    public SampleSearchResultsPanel(String title, String searchId, String category) {
        super(title, searchId, category);
    }

    public void populatePanel(SearchResultsData searchResult) {
        clear();
        CategorySummarySearchPanel summaryPanel = createSummaryPanel(searchResult);
        add(getPanelSpacer());
        // populate the summary panel
        summaryPanel.populatePanel();
        // create the sample result panel
        CategorySearchResultTablePanel resultTablePanel = createTableResults(searchResult);
        resultTablePanel.setSelectionListener(new SampleSelectedListener());
        add(resultTablePanel);
//        add(getPanelSpacer());
        // create the reads panel
//        sampleReadsTablePanel = createSampleReadsTablePanel();
//        add(sampleReadsTablePanel);
//        resultTablePanel.addDataRetrievedCallback(sampleReadsTablePanel.getSamplesRetrievedListener(resultTablePanel));
        // populate the sample results
        resultTablePanel.populatePanel();
    }

    //private SearchReadsTablePanel createSampleReadsTablePanel() {
    //    SearchReadsTablePanel sampleReadsTablePanel = new SearchReadsTablePanel();
    //    sampleReadsTablePanel.createReadDataPanel(10,new String[]{"10","20","50"});
    //    sampleReadsTablePanel.setEntityListener(_entityListener);
    //    return sampleReadsTablePanel;
    //}

}
