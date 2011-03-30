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

package org.janelia.it.jacs.web.gwt.common.client.model.metadata;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNode;

/**
 * @author Michael Press
 */
public class SampleItem implements IsSerializable, Comparable {
    private String _project;
    private Sample _sample;
    private Site _site;
    private DownloadableDataNode _dataFile;

    /**
     * Required for GWT
     */
    public SampleItem() {
    }

    public SampleItem(String project, Sample sample, DownloadableDataNode dataFile, Site site) {
        _project = project;
        _sample = sample;
        _dataFile = dataFile;
        _site = site;
    }

    public String getProject() {
        return _project;
    }

    public Sample getSample() {
        return _sample;
    }

    public Site getSite() {
        return _site;
    }

    public DownloadableDataNode getDataFile() {
        return _dataFile;
    }

    public int compareTo(Object o) {
        if (o == null) return 1;

        SampleItem other = (SampleItem) o;
        return getProject().compareTo(other.getProject());
    }
}
