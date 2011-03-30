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

package org.janelia.it.jacs.shared.lucene.searchers;

import org.apache.lucene.search.Hit;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.shared.lucene.LuceneIndexer;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Mar 6, 2008
 * Time: 1:13:04 PM
 */
public class ProjectsSearcher extends LuceneSearcherBase {

    public ProjectsSearcher() throws IOException {
        super();
    }

    public String getSearcherIndexType() {
        return LuceneIndexer.INDEX_PROJECTS;
    }

    public String getSearchTaskTopic() {
        return SearchTask.TOPIC_PROJECT;
    }

    protected String getResultTableName() {
        return "project_ts_result";
    }

    protected String getIdFieldName() {
        return "accession";
    }

    /* overriding base class - projects use accessions instead of oids */
    public void prepareStatementForHit(PreparedStatement pstmt, Hit hit, Long resultNodeId, String searchCategory)
            throws SQLException, IOException {
        pstmt.setLong(1, resultNodeId);         // node
        pstmt.setString(2, hit.get("accession"));// accession
        pstmt.setFloat(3, hit.getScore());       // rank
    }

}