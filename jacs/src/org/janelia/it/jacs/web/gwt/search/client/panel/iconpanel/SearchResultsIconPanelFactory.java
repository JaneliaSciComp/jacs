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

package org.janelia.it.jacs.web.gwt.search.client.panel.iconpanel;

import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;

/**
 * @author Michael Press
 */
public class SearchResultsIconPanelFactory extends SearchIconPanelFactory {
    public SearchResultsIconPanelFactory() {
        super();
    }

    public Object createAllSearchArtifact(Object[] args) {
        return new SearchResultsIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getSearchAllIconSmall(), Constants.SEARCH_ALL), Constants.SEARCH_ALL, null);
    }

    public Object createAccessionsSearchArtifact(Object[] args) {
        return new SearchResultsIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getAccessionIconLarge(), Constants.SEARCH_ACCESSION), Constants.SEARCH_ACCESSION, null);
    }

    public Object createProteinsSearchArtifact(Object[] args) {
        return new SearchResultsIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getProteinsIconLarge(), Constants.SEARCH_PROTEINS), Constants.SEARCH_PROTEINS, null);
    }

    public Object createClustersSearchArtifact(Object[] args) {
        return new SearchResultsIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getProteinClustersIconLarge(), Constants.SEARCH_CLUSTERS), Constants.SEARCH_CLUSTERS, null);
    }

    public Object createPublicationsSearchArtifact(Object[] args) {
        return new SearchResultsIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getPublicationsIconLarge(), Constants.SEARCH_PUBLICATIONS), Constants.SEARCH_PUBLICATIONS, null);
    }

    public Object createProjectsSearchArtifact(Object[] args) {
        return new SearchResultsIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getProjectsIconLarge(), Constants.SEARCH_PROJECTS), Constants.SEARCH_PROJECTS, null);
    }

    public Object createSamplesSearchArtifact(Object[] args) {
        return new SearchResultsIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getSamplesIconLarge(), Constants.SEARCH_SAMPLES), Constants.SEARCH_SAMPLES, null);
    }

    public Object createWebsiteSearchArtifact(Object[] args) {
        return new SearchResultsIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getWebsiteIconLarge(), Constants.SEARCH_WEBSITE), Constants.SEARCH_WEBSITE, null);
    }
}