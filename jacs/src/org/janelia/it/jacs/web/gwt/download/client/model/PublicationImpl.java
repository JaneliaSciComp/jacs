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
