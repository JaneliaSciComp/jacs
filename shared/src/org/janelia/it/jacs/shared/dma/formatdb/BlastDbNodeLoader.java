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

package org.janelia.it.jacs.shared.dma.formatdb;

import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.shared.dma.DmaArgs;
import org.janelia.it.jacs.shared.dma.util.SqlExecutor;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

/**
 * This class is responsible for loading blastable dataset nodes
 *
 * @author Tareq Nabeel
 */
public class BlastDbNodeLoader {

    public static BlastDBDirs loadBlastDBFileNodes(DmaArgs dmaArgs) throws IOException, SQLException {
        BlastDBDirs blastDBDirs = new BlastDBDirs();
        blastDBDirs.setDmaArgs(dmaArgs);
        if (dmaArgs.doBlastDBCreation()) {
            initBlastDBDirs(dmaArgs.getIdsOfBlastNodesToRegenerate(), blastDBDirs);
        }
        blastDBDirs.sortFiles();
        return blastDBDirs;

    }

    private static void populateIdsWhenAllSpecfied(Set<Long> nodeIds) throws SQLException {
        if (nodeIds.size() == 0) {    // not set by init with commandline values
            ResultSet rs = SqlExecutor.execute("select node_id, name from node where subclass in ('BlastDatabaseFileNode','DownloadableFastaFileNode') and visibility <> 'deprecated' ");
            while (rs.next()) {
                nodeIds.add(rs.getLong("node_id"));
            }
            rs.close();
        }
    }

    private static void initBlastDBDirs(Set<Long> nodeIds, BlastDBDirs blastDBDirs) throws IOException, SQLException {
        populateIdsWhenAllSpecfied(nodeIds);
        for (Long nodeId : nodeIds) {
            File fastaFileDir = FileUtil.ensureDirExists(SystemConfigurationProperties.getString("dma.blastDBFileNodePath"),
                    nodeId.toString(), true);
            blastDBDirs.addDmaFile(fastaFileDir);
        }
    }

}
