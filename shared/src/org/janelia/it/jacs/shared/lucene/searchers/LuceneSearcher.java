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

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Hit;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: May 1, 2008
 * Time: 2:00:53 PM
 */
public interface LuceneSearcher {
    /**
     * This method searches for the given string within the Lucene Document index file for the subject area in
     * question.
     *
     * @param searchString - look for this string in the Documents
     * @return - Hits of where the string was found
     * @throws java.io.IOException - could not look in and access the necessary files
     * @throws org.apache.lucene.queryParser.ParseException
     *                             - had trouble parsing the information
     */
    public Hits search(String searchString) throws IOException, ParseException;

    public TopDocs search(String searchString, int maxDocs) throws IOException, ParseException;

    /* next 2 methods are for direct JDBC record inserts */
    public String getPreparedStatement();

    public void prepareStatementForHit(PreparedStatement pstmt, Hit hit, Long resultNodeId, String searchCategory)
            throws SQLException, IOException;

    /* next 2 methods are for Postgres COPY command load */
    public String getCopyCommand(String path);

    public String writeRecordForHit(Hit hit, Long resultNodeId) throws IOException;

    public String writeRecordForDoc(ScoreDoc scoreDoc, Long resultNodeId) throws IOException;

}
