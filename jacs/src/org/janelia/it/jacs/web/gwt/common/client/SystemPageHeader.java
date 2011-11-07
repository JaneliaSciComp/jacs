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

package org.janelia.it.jacs.web.gwt.common.client;

import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.core.BrowserDetector;
import org.janelia.it.jacs.web.gwt.common.client.core.PlatformDetector;
import org.janelia.it.jacs.web.gwt.common.client.security.ClientSecurityUtils;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.HelpActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.common.client.ui.menu.SimpleMenu;
import org.janelia.it.jacs.web.gwt.common.client.ui.menu.SimpleMenuBar;
import org.janelia.it.jacs.web.gwt.common.client.ui.menu.SimpleMenuItem;
import org.janelia.it.jacs.web.gwt.common.client.ui.window.OpenInNewWindowCommand;
import org.janelia.it.jacs.web.gwt.common.client.ui.window.OpenInSameWindowCommand;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.SystemProps;
import org.janelia.it.jacs.web.gwt.common.client.util.UrlBuilder;

/**
 * @author Michael Press
 */
public class SystemPageHeader {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.header.client.Header");

    private static String HEADER_DICTIONARY_NAME = "headerValues";
    private static final String DEFAULT_URL = "http://www.jcvi.org"; // default if no prefs found (shouldn't happen ;-)
    private static Dictionary _prefs = Dictionary.getDictionary(HEADER_DICTIONARY_NAME);

    // These values match JavaScript array keys in Header.jsp.  GWT will find them via the "headerValues" Dictionary
    private static final String HOME_URL_PREF = "HomeURL";
    private static final String LOGIN_URL_PREF = "LoginURL";
    private static final String LOGOUT_URL_PREF = "LogoutURL";
    private static final String HELP_URL_PREF = "HelpURL";
    private static TextBox _searchTextBox;
    private static VerticalPanel _searchPanel = new VerticalPanel();

    public static Panel getHeader() {
        if (_logger.isDebugEnabled()) {
            _logger.debug("platform is " + PlatformDetector.platform());
            _logger.debug("user agent is " + BrowserDetector.getUserAgent());
        }

        Image logo = ImageBundleFactory.getControlImageBundle().getHeaderLogoImage().createImage();
        logo.addClickListener(new OpenInSameWindowCommand(getValue(HOME_URL_PREF)));
        logo.setStyleName("HeaderLogo");

        HorizontalPanel upperHeader = new HorizontalPanel();
        upperHeader.setWidth("100%");
        upperHeader.setHorizontalAlignment(HorizontalPanel.ALIGN_LEFT);
        upperHeader.setStyleName("HeaderPanel");
        upperHeader.add(logo);
        upperHeader.add(getLinkPanel());

        HorizontalPanel lowerHeader = new HorizontalPanel();
        lowerHeader.setHorizontalAlignment(HorizontalPanel.ALIGN_LEFT);
        lowerHeader.setVerticalAlignment(HorizontalPanel.ALIGN_BOTTOM);
        lowerHeader.setStyleName("HeaderTopLevelMenuItem");
        lowerHeader.add(getMenuBar());
        lowerHeader.add(getSearchPanel());

        VerticalPanel header = new VerticalPanel();
        header.setHorizontalAlignment(VerticalPanel.ALIGN_LEFT);
        header.setStyleName("HeaderPanel");
        header.add(upperHeader);
        header.add(lowerHeader);
        return header;
    }

    private static Widget getMenuBar() {
        // This is a very brute force method to prevent people from firing off the metagenomic pipeline from certain servers
        boolean showMetagenomicPipeline = SystemProps.getBoolean("MgAnnotation.Server", false);
        SimpleMenuBar menubar = new SimpleMenuBar();
        menubar.addItem(new SimpleMenuItem("Home", new OpenInSameWindowCommand(UrlBuilder.getResearchHomeUrl())));
        menubar.addMenu(new SimpleMenu(getMenuLabel("Tools"), new SimpleMenuItem[]{
                new SimpleMenuItem("BLAST (N, X, P)", new OpenInSameWindowCommand(UrlBuilder.getAdvancedBlastUrl())),
                new SimpleMenuItem("PSI-BLAST", new OpenInSameWindowCommand(UrlBuilder.getPsiBlastUrl())),
                new SimpleMenuItem("Reverse PSI-BLAST", new OpenInSameWindowCommand(UrlBuilder.getReversePsiBlastUrl())),
                new SimpleMenuItem("Fragment Recruitment Viewer", new OpenInSameWindowCommand(UrlBuilder.getFrvUrl())),
                new SimpleMenuItem("My Job Results", new OpenInSameWindowCommand(UrlBuilder.getStatusUrl()))
        }));

        SimpleMenu pipelineMenu = new SimpleMenu(getMenuLabel("Pipelines"), new SimpleMenuItem[]{});
        pipelineMenu.addItem(new SimpleMenuItem("Neuron Separator", new OpenInSameWindowCommand(UrlBuilder.getNeuronSeparatorPipeline())));
        pipelineMenu.addItem(new SimpleMenuItem("16S/18S Small Sub-Unit Analysis", new OpenInSameWindowCommand(UrlBuilder.getAnalysisPipeline16SUrl())));
        pipelineMenu.addItem(new SimpleMenuItem(Constants.BARCODE_LABEL, new OpenInSameWindowCommand(UrlBuilder.getBarcodeDesignerUrl())));
        pipelineMenu.addItem(new SimpleMenuItem(Constants.FR_LABEL, new OpenInSameWindowCommand(UrlBuilder.getFRPipelineUrl())));
        pipelineMenu.addItem(new SimpleMenuItem("Inspect Peptide Mapper", new OpenInSameWindowCommand(UrlBuilder.getInspectUrl())));
        if (showMetagenomicPipeline) {
            pipelineMenu.addItem(new SimpleMenuItem("Metagenomics Annotation", new OpenInSameWindowCommand(UrlBuilder.getMgAnnotationUrl())));
        }
        pipelineMenu.addItem(new SimpleMenuItem("Prokaryotic Annotation", new OpenInSameWindowCommand(UrlBuilder.getProkAnnotationUrl())));
//                new SimpleMenuItem("Degenerate Primer Design",    new OpenInSameWindowCommand(UrlBuilder.getDegeneratePrimerDesignUrl())),
        pipelineMenu.addItem(new SimpleMenuItem(Constants.RNA_SEQ_PIPELINE_LABEL, new OpenInSameWindowCommand(UrlBuilder.getRnaSeqPipelineUrl())));
        pipelineMenu.addItem(new SimpleMenuItem("Sequence Profile Comparison Tool", new OpenInSameWindowCommand(UrlBuilder.getSequenceProfileComparisonUrl())));
//                new SimpleMenuItem("Inter-Site Comparison Tool",  new OpenInSameWindowCommand(UrlBuilder.getIntersiteComparisonToolUrl())),
//                new SimpleMenuItem("Primer Design For Closure",   new OpenInSameWindowCommand(UrlBuilder.getClosurePrimerDesignUrl())),
        menubar.addMenu(pipelineMenu);

        menubar.addMenu(new SimpleMenu(getMenuLabel("Projects"), new SimpleMenuItem[]{
                new SimpleMenuItem("GECI Image Processing", new OpenInSameWindowCommand(UrlBuilder.getNeuronalAssayAnalysisUrl())),
                new SimpleMenuItem("Zlatic Lab", new OpenInSameWindowCommand(UrlBuilder.getZlaticLabUrl())),
        }));

        menubar.addMenu(new SimpleMenu(getMenuLabel("Example Functionality"), new SimpleMenuItem[]{
                new SimpleMenuItem("Browse Projects", new OpenInSameWindowCommand(UrlBuilder.getProjectsUrl())),
                new SimpleMenuItem("Browse Publications", new OpenInSameWindowCommand(UrlBuilder.getPubsUrl())),
                new SimpleMenuItem("Browse Samples", new OpenInSameWindowCommand(UrlBuilder.getSamplesUrl())),
                new SimpleMenuItem("Search", new OpenInSameWindowCommand(UrlBuilder.getSearchUrl()))
        }));

        if (ClientSecurityUtils.isAdmin()) {
            menubar.addMenu(new SimpleMenu(getMenuLabel("Admin"), new SimpleMenuItem[]{
                    new SimpleMenuItem("Task Report", new OpenInSameWindowCommand(UrlBuilder.getTaskReportUrl())),
                    new SimpleMenuItem("Blast Report", new OpenInSameWindowCommand(UrlBuilder.getBlastReportUrl())),
                    new SimpleMenuItem("Disk Usage Report", new OpenInSameWindowCommand(UrlBuilder.getDiskUsageReportUrl())),
                    new SimpleMenuItem("Health Monitor", new OpenInNewWindowCommand(UrlBuilder.getHealthMonitorUrl()))
            }));
        }
        return menubar;
    }

    private static HTML getMenuLabel(String buttonLabel) {
        return new HTML(buttonLabel + ImageBundleFactory.getControlImageBundle().getArrowDownEnabledImage().getHTML());
    }

    private static String getValue(String name) {
        if (_prefs != null)
            return _prefs.get(name);
        return DEFAULT_URL;
    }

    private static Widget getLinkPanel() {
        HorizontalPanel panel = new HorizontalPanel();
        panel.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);
        panel.setStyleName("HeaderLinkPanel");

        if (ClientSecurityUtils.isAuthenticated()) {
            Link logoutLink = new Link("Log out", new OpenInSameWindowCommand(getValue(LOGOUT_URL_PREF)));
            logoutLink.setHyperlinkStyleName("HeaderLink");
            panel.add(logoutLink);
            panel.add(HtmlUtils.getHtml("|", "HeaderLinkSeparator"));

            _searchPanel.setStyleName("HeaderSearchPanelLoggedIn" + (PlatformDetector.isMac() ? "Mac" : ""));
        }
        else {
            Link loginLink = new Link("Log in", new OpenInSameWindowCommand(getValue(LOGIN_URL_PREF)));
            loginLink.setHyperlinkStyleName("HeaderLink");
            panel.add(loginLink);
            panel.add(HtmlUtils.getHtml("|", "HeaderLinkSeparator"));

            _searchPanel.setStyleName("HeaderSearchPanelLoggedOut" + (PlatformDetector.isMac() ? "Mac" : ""));
        }

        HelpActionLink helpLink = new HelpActionLink("Help", getValue(HELP_URL_PREF));
        helpLink.setLinkStyleName("HeaderLink");
        helpLink.addImageStyleName("HeaderHelpImage");
        panel.add(helpLink);

        return panel;
    }

    private static Widget getSearchPanel() {
        _searchTextBox = new TextBox();
        _searchTextBox.setVisibleLength(25);
        _searchTextBox.setStyleName("HeaderSearchTextBox");

        Button searchButton = new Button("Search", new SearchCommand());
        searchButton.setStyleName("HeaderSearchButton");

        HorizontalPanel row1 = new HorizontalPanel();
        row1.setStyleName("HeaderTopLevelMenuItem");
        row1.add(_searchTextBox);
        row1.add(HtmlUtils.getHtml("&nbsp;", "HeaderSearchButtonSpacer"));
        row1.add(searchButton);

        _searchPanel.add(row1);

        return _searchPanel;
    }

    private static class SearchCommand extends OpenInSameWindowCommand {
        private SearchCommand() {
            super(null);
        }

        public void onClick(Widget sender) {
            setUrl(UrlBuilder.getSearchUrl() + "?keyword=" + _searchTextBox.getText());
            super.onClick(sender);
        }
    }

}
