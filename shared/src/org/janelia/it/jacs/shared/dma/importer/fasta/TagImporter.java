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

package org.janelia.it.jacs.shared.dma.importer.fasta;

import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;
import org.janelia.it.jacs.model.dma.Tag;
import org.janelia.it.jacs.shared.dma.entity.SequenceInfo;
import org.janelia.it.jacs.shared.dma.entity.SequenceInfos;
import org.janelia.it.jacs.shared.dma.reporter.DmaLogger;
import org.janelia.it.jacs.shared.dma.util.ConnPool;
import org.janelia.it.jacs.shared.perf.PerfStats;

import java.sql.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class is responsible for  importing new Tags that are parsed in fasta files.
 *
 * @author Tareq Nabeel
 */
public class TagImporter {
    private static Map<String, Tag> existingTags;
    private static Map<String, Tag> newTags;
    private static Map<String, Tag> errorTags;
    private static final String TAG_ID = "tag_id";
    private static final String TAG_NAME = "name";

    private static DmaLogger dmaLogger = DmaLogger.getInstance();

    private static final String INSERT_TAG_STMT = "insert into dma_tag (tag_id,name,description,classification_id,save_tag,entity_sql) values (?,?,?,?,?,?)";

    static {
        loadTags();
    }

    /**
     * This method loads all tags into memory for better performance
     */
    private static void loadTags() {
        PerfStats.start(PerfStats.KEY_LOAD_ALL_TAGS_IN_CACHE);
        try {
            existingTags = new HashMap<String, Tag>();
            newTags = new HashMap<String, Tag>();
            errorTags = new HashMap<String, Tag>();
            Connection conn = ConnPool.getConnection();
            conn.setAutoCommit(false);
            Statement findAllTagStmt = conn.createStatement();
            ResultSet rs = findAllTagStmt.executeQuery("select t.tag_id,t.name from dma_tag t");
            while (rs.next()) {
                long id = rs.getLong(TAG_ID);
                String name = rs.getString(TAG_NAME);
                Tag tag = new Tag();
                tag.setName(name);
                tag.setId(id);
                existingTags.put(name, tag);
            }
            findAllTagStmt.close();
            ConnPool.releaseConnection(conn);
        }
        catch (SQLException e) {
            throw new ExceptionInInitializerError(e);
        }
        PerfStats.end(PerfStats.KEY_LOAD_ALL_TAGS_IN_CACHE);
    }

    /**
     * This method can get called by multiple threads concurrently.  It filters out tags in sequenceInfos
     * that already exist in memory to form final insert set
     * We need to synchronize the call as it accesses shared newTags and existingTags static variables
     *
     * @param conn
     * @param sequenceInfos the parsed sequences from fasta file
     * @throws SQLException
     */
    public synchronized static void importNewTags(Connection conn, SequenceInfos sequenceInfos) throws SQLException {
        PerfStats.start(PerfStats.KEY_IMPORT_NEW_TAGS);
        try {
            addTags(sequenceInfos);
            newTags.values().removeAll(errorTags.values());
            if (newTags.size() == 0) {
                return;
            }
            PreparedStatement insertTagsPstmt = conn.prepareStatement(INSERT_TAG_STMT);
            Iterator<Long> idsIter = TimebasedIdentifierGenerator.generateIdList(newTags.size()).iterator();
            for (Tag tag : newTags.values()) {
                tag.setId(idsIter.next());
                insertTagsPstmt.setLong(1, tag.getId());
                insertTagsPstmt.setString(2, tag.getName());
                insertTagsPstmt.setString(3, null);
                insertTagsPstmt.setLong(4, -1);
                insertTagsPstmt.setBoolean(5, true);
                insertTagsPstmt.setString(6, "select entity_id from dma_tag_entity where tag_id=" + tag.getId());
                insertTagsPstmt.addBatch();
            }
            insertTagsPstmt.executeBatch();
            insertTagsPstmt.clearParameters();
            insertTagsPstmt.clearBatch();
            dmaLogger.addTagInserts(newTags.size());
            existingTags.putAll(newTags);
            newTags.clear();
        }
        catch (SQLException e) {
            errorTags.putAll(newTags);
            throw e;
        }
        PerfStats.end(PerfStats.KEY_IMPORT_NEW_TAGS);
    }

    /**
     * Filters out tags in sequenceInfos that already exist in memory to form final insert set (newTags).
     * We need to synchronize the call as it accesses shared newTags and existingTags static variables
     *
     * @param sequenceInfos the parsed sequences from fasta file
     */
    private synchronized static void addTags(SequenceInfos sequenceInfos) {
        for (SequenceInfo sequenceInfo : sequenceInfos.getParsedSequenceInfos().values()) {
            for (String parsedTag : sequenceInfo.getParsedTags()) {
                parsedTag = parsedTag.trim().toLowerCase();
                Tag tag = existingTags.get(parsedTag);
                if (tag == null) {
                    if (newTags.get(parsedTag) == null) {
                        tag = new Tag(parsedTag);
                        tag.setId((Long) TimebasedIdentifierGenerator.generate(1));
                        newTags.put(tag.getName(), tag);
                        dmaLogger.logNewTag(tag);
                    }
                }
            }
        }
    }

    public synchronized Map<String, Tag> getExistingTags() {
        return existingTags;
    }

}
