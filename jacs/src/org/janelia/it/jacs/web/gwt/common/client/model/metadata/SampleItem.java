
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
