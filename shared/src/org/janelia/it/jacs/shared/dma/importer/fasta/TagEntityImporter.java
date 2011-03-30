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

import org.janelia.it.jacs.model.dma.Tag;
import org.janelia.it.jacs.shared.dma.entity.SequenceInfo;
import org.janelia.it.jacs.shared.dma.entity.SequenceInfos;
import org.janelia.it.jacs.shared.perf.PerfStats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * This class is responsible for creating associations between dma_tag and sequence_entity
 * tables.  Currently, because all dma_tag records have a custom query defined (which wasn't the case
 * in the beginning), this class isn't being used in the FastaImporter
 *
 * @author Tareq Nabeel
 */
public class TagEntityImporter {

    private PreparedStatement insertTagEntitiesPstmt;

    private static final String INSERT_TAG_ENTITIES_STMT = "insert into dma_tag_entity (entity_id,tag_id) values (?,?)";

    public TagEntityImporter(Connection conn) throws SQLException {
        this.insertTagEntitiesPstmt = conn.prepareStatement(INSERT_TAG_ENTITIES_STMT);
    }

    /**
     * This method creates dma_tag table records thereby forming association between tags and sequence_entity
     * records. Currently, because all dma_tag records have a custom query defined (which wasn't the case
     * in the beginning), this method isn't being called by FastaImporter
     *
     * @param sequenceInfos
     * @throws SQLException
     */
    public void createTagLinksForNewSequences(SequenceInfos sequenceInfos) throws SQLException {
        PerfStats.start(PerfStats.KEY_CREATE_TAG_LINKS_FOR_NEW_SEQUENCES);
        if (sequenceInfos.getImportSequenceInfos().size() == 0) {
            return;
        }
        for (SequenceInfo seqInfo : sequenceInfos.getImportSequenceInfos().values()) {
            for (Tag tag : seqInfo.getTagsSet()) {
                insertTagEntitiesPstmt.setLong(1, seqInfo.getEntityId());
                insertTagEntitiesPstmt.setLong(2, tag.getId());
                insertTagEntitiesPstmt.addBatch();
            }
        }
        // form relationships between existing tags and new entities
        insertTagEntitiesPstmt.executeBatch();
        insertTagEntitiesPstmt.clearParameters();
        insertTagEntitiesPstmt.clearBatch();
        PerfStats.end(PerfStats.KEY_CREATE_TAG_LINKS_FOR_NEW_SEQUENCES);
    }
}
