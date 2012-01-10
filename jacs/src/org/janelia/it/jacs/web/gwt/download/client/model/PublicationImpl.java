
package org.janelia.it.jacs.web.gwt.download.client.model;

import org.janelia.it.jacs.model.download.Author;
import org.janelia.it.jacs.model.download.DataFile;
import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNode;
import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNodeImpl;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Aug 21, 2006
 * Time: 11:51:32 AM
 * <p/>
 * Implementation of the model interface.  Simple one to allow set/get.
 */
public class PublicationImpl implements Publication {

    private String accessionNumber;
    private String title;
    private String summary;
    private String publicationAbstract;

    private List<Author> authors;
    private List<DownloadableDataNodeImpl> dataFiles;
    private DownloadableDataNode subjectDocument;
    private List<DataFile> rolledUpArchives;
    private String description;

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getAbstract() {
        return publicationAbstract;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAbstract(String publicationAbstract) {
        this.publicationAbstract = publicationAbstract;
    }

    public List<Author> getAuthors() {
        return authors;
    }

    public void setAuthors(List<Author> authors) {
        this.authors = authors;
    }

    public List<DownloadableDataNodeImpl> getDataFiles() {
        return dataFiles;
    }

    public void setDataFiles(List<DownloadableDataNodeImpl> dataFiles) {
        this.dataFiles = dataFiles;
    }

    public DownloadableDataNode getSubjectDocument() {
        return subjectDocument;
    }

    public void setSubjectDocument(DownloadableDataNode doc) {
        subjectDocument = doc;
    }

    public List<DataFile> getRolledUpDataArchives() {
        return rolledUpArchives;
    }

    public void setRolledUpDataArchives(List<DataFile> archives) {
        rolledUpArchives = archives;
    }

}
