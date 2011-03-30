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

package org.janelia.it.jacs.shared.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.shared.lucene.data_retrievers.LuceneDataRetrieverBase;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Mar 5, 2008
 * Time: 2:43:11 PM
 */
public class LuceneIndexer {

    public static final String INDEX_ALL = "all";
    public static final String INDEX_CLUSTERS = "final_cluster";
    public static final String INDEX_PROJECTS = "project";
    public static final String INDEX_PROTEINS = "protein";
    public static final String INDEX_PUBLICATIONS = "publication";
    public static final String INDEX_SAMPLES = "sample";

    public final static int MERGE_FACTOR = 1000;
    public final static int MAX_MERGE_DOCUMENTS = Integer.MAX_VALUE;
    public static final Set<String> SET_OF_ALL_DOC_TYPES;

    static {
        SET_OF_ALL_DOC_TYPES = new HashSet<String>();
        SET_OF_ALL_DOC_TYPES.add(INDEX_CLUSTERS);
        SET_OF_ALL_DOC_TYPES.add(INDEX_PROJECTS);
        SET_OF_ALL_DOC_TYPES.add(INDEX_PROTEINS);
        SET_OF_ALL_DOC_TYPES.add(INDEX_PUBLICATIONS);
        SET_OF_ALL_DOC_TYPES.add(INDEX_SAMPLES);
    }

    /**
     * Method used to generate a searchable index files for a given set Document types.
     *
     * @param docTypeSet             - list of doc types to generate indexes for
     * @param numberOfRecordsToIndex - number of records to Lucene to index
     * @throws Exception - problem generating the indexes
     */
    public void execute(Set<String> docTypeSet, int numberOfRecordsToIndex) throws Exception {
        for (String docType : docTypeSet) {
            IndexWriter writer = openIndex(getIndexRootPath() + docType);
            Connection conn = openDbConnection(true);
            LuceneDataRetrieverBase dr = LuceneDataFactory.createDocumentRetriever(docType);
            dr.setConnection(conn);
            SimpleOut.sysOut("Begin retriving and indexing");
            if (numberOfRecordsToIndex > 0) {
                dr.setSetSize(numberOfRecordsToIndex);
            }
            String dumpFileName = getIndexRootPath() + docType + ".txt";
            dr.executeDbDump(dumpFileName);
            dr.processDocumentsFromDbFile(new File(dumpFileName), writer);
            //dr.deleteDbDumpFile(dumpFileName);
            SimpleOut.sysOut("Done. Total " + dr.getTotalRecordsProcessed() + " records");
            conn.close();
            writer.optimize();
            SimpleOut.sysOut("Finished index optimization");
            writer.close();
        }
    }


    /**
     * Open the index file for business
     *
     * @param idxPath - path to the specific index file being generated
     * @return - a handle to the writer object
     * @throws IOException - error working with the index file
     */
    private IndexWriter openIndex(String idxPath) throws IOException {
        Analyzer analyzer = new StandardAnalyzer();
        boolean createFlag = true;
        SimpleOut.sysOut("Opening index at <" + idxPath + ">");
        IndexWriter writer = new IndexWriter(idxPath, analyzer, createFlag);
        writer.setMergeFactor(MERGE_FACTOR);
        writer.setMaxMergeDocs(MAX_MERGE_DOCUMENTS);
        return writer;
    }


    /**
     * Opens and returns a connection to the db
     *
     * @param autoCommit - establishes whether the connection will autocommit changes
     * @return - the connection to the db
     * @throws SQLException           - error enacting SQL in the db
     * @throws ClassNotFoundException - unable to find the db Driver class
     */
    public static Connection openDbConnection(boolean autoCommit) throws SQLException, ClassNotFoundException {
        Class.forName(SystemConfigurationProperties.getString("jdbc.driverClassName"));
        Connection connection = DriverManager.getConnection(
                SystemConfigurationProperties.getString("jdbc.url") + "?" +
                        "user=" + SystemConfigurationProperties.getString("ts.jdbc.username") + "&" +
                        "password=" + SystemConfigurationProperties.getString("ts.jdbc.password")
        );
        connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        connection.setAutoCommit(autoCommit);
        SimpleOut.sysOut("Connected to DB");
        return connection;
    }

    /**
     * This method returns the path to the index dir
     *
     * @return - string path to the index dir
     */
    public static String getIndexRootPath() {
        return SystemConfigurationProperties.getString("ts.indexRootPath");
    }


    /**
     * Gets called by dma.sh with various arguments
     *
     * @param args command line args
     * @see IndexerArgs
     */
    public static void main(String[] args) {
        LuceneIndexer indexer = new LuceneIndexer();
        try {
            IndexerArgs iArgs = new IndexerArgs(args);
            indexer.execute(iArgs.getDocTypesToIndex(), iArgs.getRecsCount());
            SimpleOut.sysOut("Done");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
