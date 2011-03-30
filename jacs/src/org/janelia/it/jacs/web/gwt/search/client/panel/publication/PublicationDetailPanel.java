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

package org.janelia.it.jacs.web.gwt.search.client.panel.publication;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.download.client.DownloadMetaDataService;
import org.janelia.it.jacs.web.gwt.download.client.DownloadMetaDataServiceAsync;
import org.janelia.it.jacs.web.gwt.download.client.PublicationPanelHelper;
import org.janelia.it.jacs.web.gwt.download.client.model.Publication;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 9, 2007
 * Time: 4:42:32 PM
 */
public class PublicationDetailPanel extends TitledBox {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.search.client.panel.publication.PublicationDetailPanel");

    private Publication _publication;
    private ActionLink _actionLink;
    private PublicationPanelHelper _panelBuildHelper;
    private LoadingLabel _loadingLabel;

    private static DownloadMetaDataServiceAsync downloadService =
            (DownloadMetaDataServiceAsync) GWT.create(DownloadMetaDataService.class);

    static {
        ((ServiceDefTarget) downloadService).setServiceEntryPoint("download.oas");
    }

    public PublicationDetailPanel() {
        super("", false /*show action links*/, false /*show content*/);
    }

    public PublicationDetailPanel(String title, Publication publication, ActionLink actionLink, boolean showActionLinks) {
        super(title,
                false, /*showActionLinks*/
                true /*show content*/);
        init(null, publication, actionLink, showActionLinks);
    }

    public PublicationDetailPanel(String title, String publicationAcc, ActionLink actionLink, boolean showActionLinks) {
        super(title,
                false, /*showActionLinks*/
                true /*show content*/);
        init(publicationAcc, null, actionLink, showActionLinks);
    }

    private void init(String publicationAcc, Publication publication, ActionLink actionLink, boolean showActionLinks) {
        _actionLink = actionLink;
        if (showActionLinks) {
            setShowActionLinks(true);
            showActionLinks();
        }
        _loadingLabel = new LoadingLabel("Loading publication...", true);
        add(_loadingLabel);
        _panelBuildHelper = new PublicationPanelHelper();
        if (publication != null) {
            _publication = publication;
            // populate the panel
            localPopulateContentPanel(_publication);
        }
        else if (publicationAcc != null) {
            // retrieve the publication
            retrievePublication(publicationAcc);
        }
    }

    private void localPopulateContentPanel(Publication publication) {
        // publication panel actually is a tab panel that
        // contains publication details and data download links
        Widget pubPanel = _panelBuildHelper.createPublicationPanel(publication, null);
        if (_actionLink != null && getShowActionLinks()) {
            addActionLink(_actionLink);
        }
        this.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        this.add(pubPanel);
    }

    private void retrievePublication(final String publicationAccession) {
        _logger.debug("Retrieve publication: " + publicationAccession);
        downloadService.getPublicationByAccession(publicationAccession, new AsyncCallback() {
            public void onFailure(Throwable throwable) {
                _logger.error(throwable);
                _loadingLabel.setVisible(false);
                add(HtmlUtils.getHtml("Error retrieving the publication " + publicationAccession,
                        "error"));
            }

            public void onSuccess(Object result) {
                Publication publication = (Publication) result;
                _loadingLabel.setVisible(false);
                if (publication != null) {
                    _publication = publication;
                    localPopulateContentPanel(publication);
                }
                else {
                    // no publication found
                    add(HtmlUtils.getHtml("No publication found for " + publicationAccession,
                            "text"));
                }
            }
        });
    }

}
