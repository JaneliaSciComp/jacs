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

package org.janelia.it.jacs.web.gwt.search.client.page;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardController;
import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardPage;
import org.janelia.it.jacs.web.gwt.search.client.model.SearchResultsData;
import org.janelia.it.jacs.web.gwt.search.client.service.SearchService;
import org.janelia.it.jacs.web.gwt.search.client.service.SearchServiceAsync;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 28, 2007
 * Time: 4:56:54 PM
 */
public abstract class SearchWizardPage extends WizardPage {
    public static final String DETAIL_URL = "/jacs/gwt/Search/SearchEntityDetailsPage.htm";
    protected static SearchServiceAsync _searchService = (SearchServiceAsync) GWT.create(SearchService.class);

    public SearchResultsData _data;

    static {
        ((ServiceDefTarget) _searchService).setServiceEntryPoint("search.oas");
    }

    public SearchWizardPage(SearchResultsData data, WizardController controller) {
        super(controller);
        _data = data;
    }

    public SearchWizardPage(SearchResultsData data, WizardController controller, boolean showButtons) {
        super(showButtons, controller);
        _data = data;
    }

    public SearchResultsData getData() {
        return _data;
    }


    abstract public String getPageToken(); // used for history

    abstract public Widget getMainPanel();

    abstract public String getPageTitle();

    protected void preProcess(Integer priorPageNumber) {
    }

    protected void setupButtons() {
        getButtonManager().getBackButton().setVisible(false);
        getButtonManager().getNextButton().setVisible(false);
    }
}
