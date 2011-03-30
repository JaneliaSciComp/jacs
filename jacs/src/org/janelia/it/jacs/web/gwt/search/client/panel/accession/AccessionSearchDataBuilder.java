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

package org.janelia.it.jacs.web.gwt.search.client.panel.accession;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.web.gwt.common.client.security.ClientSecurityUtils;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.NotLoggedInLink;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.search.client.model.AccessionResult;
import org.janelia.it.jacs.web.gwt.search.client.panel.CategorySearchDataBuilder;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 29, 2007
 * Time: 10:54:25 AM
 */
public class AccessionSearchDataBuilder extends CategorySearchDataBuilder {
    private static final String DATA_PANEL_TITLE = "Matching Accession";

    private HorizontalPanel dataPanel;

    public AccessionSearchDataBuilder(String searchId, String searchQuery) {
        super(searchId, searchQuery);
    }

    protected String getPanelSearchCategory() {
        return SearchTask.TOPIC_ACCESSION;
    }

    public Panel createDataPanel() {
        dataPanel = new HorizontalPanel();
        return dataPanel;
    }

    public void populateDataPanel() {
        retrieveAccessionResult();
    }

    protected String createDataPanelTitle() {
        return DATA_PANEL_TITLE;
    }

    private void retrieveAccessionResult() {
        _searchService.getPagedCategoryResults(searchId, getPanelSearchCategory(), 0, 1, new SortArgument[0],
                new AsyncCallback() {
                    public void onFailure(Throwable caught) {
                        HTML errorHTML = HtmlUtils.getHtml("Failed to retrieve accession search result", "error");
                        dataPanel.add(errorHTML);
                    }

                    public void onSuccess(Object result) {
                        List resultList = (List) result;
                        if (resultList != null && resultList.size() > 0) {
                            AccessionResult accResult = (AccessionResult) resultList.get(0);
                            String accessiontText = "Your search matched " +
                                    accResult.getAccessionType() + " Accession ";
                            HTML accessionHTMLText = HtmlUtils.getHtml(accessiontText, "text");
                            Widget accessionLink;
                            if (accResult.getAccessionType().equals("Project") ||
                                    accResult.getAccessionType().equals("Publication") ||
                                    ClientSecurityUtils.isAuthenticated()) {
                                accessionLink = getAccessionLink(accResult.getDescription(), accResult.getAccession());
                            }
                            else {
                                accessionLink = new NotLoggedInLink(accResult.getDescription());
                            }
                            dataPanel.add(accessionHTMLText);
                            dataPanel.add(HtmlUtils.getHtml("&nbsp;", "text"));
                            dataPanel.add(accessionLink);
                        }
                    }
                });
    }

}
