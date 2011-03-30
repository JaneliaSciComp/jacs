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

package org.janelia.it.jacs.shared.lucene.data_retrievers;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.janelia.it.jacs.shared.lucene.SimpleOut;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Mar 5, 2008
 * Time: 6:02:14 PM
 */
public abstract class LuceneDataRetrieverBase {

    public static final int TEST_SET_SIZE = 51981;
    protected int setSize = TEST_SET_SIZE; // default
    protected int numOfDbChunks;
    protected int dbChunk;
    protected int offset;
    protected long testId;
    protected long initTestId;
    protected long totalRecordsProcessed;

    protected ResultSet rs = null;
    protected PreparedStatement statement = null;
    protected Connection connection = null;

    public void DataRetriever() {
        Date dt = new Date();
        initTestId = dt.getTime();
        testId = initTestId;
    }

    public int getSetSize() {
        return setSize;
    }

    public void setSetSize(int setSize) {
        this.setSize = setSize;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Integer processNextResultChunk(int chunkSize, IndexWriter writer) {
        if (this.connection == null) {
            return null;
        }

        try {
            // The first execution, set up the statement and number of loops
            if (this.dbChunk == 0) {
                this.numOfDbChunks = getChunksCount(chunkSize);
            }

            // If not exceeding the total number of chunks, get data
            if (this.dbChunk < this.numOfDbChunks) {
                String sql = getSQLQueryForDocumentData();
                this.statement = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                SimpleOut.sysOut("Executing SQL chunk " + this.dbChunk);
                this.rs = statement.executeQuery();
                SimpleOut.sysOut("\texecuteQuery completed");
                List<Document> list = extractDocumentsFromResultSet();
                totalRecordsProcessed += list.size();
                for (Document doc : list) {
                    writer.addDocument(doc);
                }
                SimpleOut.sysOut("\tProcessed " + totalRecordsProcessed + " records so far. Just constructed " + list.size() + " documents");
                done();
                offset += chunkSize;
                dbChunk++;
                return list.size();
            }
            // no more to return
            else {
                return 0;
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private int getChunksCount(int chunkSize) throws SQLException {
        String sql = "select count(*) from " + getDatabaseDocumentTableName() + " ";
        Statement st = connection.createStatement();
        ResultSet r = st.executeQuery(sql);
        int cnt = 0;
        if (r.next()) {
            cnt = r.getInt(1);
        }
        else {
            SimpleOut.sysOut("====ERROR: Unable to retrieve max chunks from DB====");
        }
        r.close();
        st.close();
        if (cnt / chunkSize <= 1) {
            return 1;
        }
        else {
            return cnt / chunkSize;
        }
    }

    abstract public String getDatabaseDocumentTableName();

    abstract public String getSQLQueryForDocumentData();

    abstract public List<Document> extractDocumentsFromResultSet() throws SQLException;

    public String getStringFromResult(ResultSet result, int index) throws SQLException {
        String tmpString = result.getString(index);
        if (null == tmpString) {
            tmpString = "";
        }
        return tmpString;
    }

    public String getStringFromSplit(String piece) throws SQLException {
        if (null == piece) {
            return "";
        }
        return piece;
    }

    private void done() {
        try {
            if (rs != null)
                rs.close();
            if (statement != null)
                statement.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public long getTotalRecordsProcessed() {
        return totalRecordsProcessed;
    }

    public void executeDbDump(String dumpFileName) throws SQLException {
        String sql = "select writequeryresultstodisk(?,?,'\t')";
        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setString(1, getSQLQueryForDocumentData());
        pstmt.setString(2, dumpFileName);
        pstmt.execute();
    }

    abstract public void processDocumentsFromDbFile(File dbDumpFile, IndexWriter writer) throws IOException;

    public void deleteDbDumpFile(String dumpFileName) {
        new File(dumpFileName).delete();
    }
}
