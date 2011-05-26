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

package org.janelia.it.jacs.web.gwt.test.client;

import com.google.gwt.core.client.EntryPoint;
import org.janelia.it.jacs.model.genomics.EntityTypeGenomic;
import org.janelia.it.jacs.model.tasks.blast.*;
import org.janelia.it.jacs.model.user_data.blast.Blastable;

/**
 * This dummy page is used by the build to make sure that model classes referenced in it pass GWT compiler.
 * That in turn increases the likelihood that these model classes can be used in GWT client
 * pages in the future if they're not being used already.
 * <p/>
 * Note: To make sure that these model classes can be used at runtime by GWT, you'd have to call
 * cleanGWTEnity in the service impl layer before passing the object to GWT page.
 * <p/>
 * see org.janelia.it.jacs.web.gwt.detail.server.bse.BSEntityServiceImpl for example:
 * Read read = featureDAO.getReadByBseEntityId(Long.valueOf(bseEntityId));
 * cleanForGWT(read);
 * return read;
 */
public class CleanGWTEntities implements EntryPoint {

    /**
     * Classes below that have been commented out do not pass GWT compiler and as such cannot be referenced
     * in GWT pages directly.  In order to access any data contained by these classes we'd have to either
     * clean up GWT errors in these classes or create value objects that do pass the GWT compiler
     */

//    private org.janelia.it.jacs.model.TimebasedIdentifierGenerator idGenerator;
//    private org.janelia.it.jacs.server.utils.BaseProperties baseProperties;
//    private org.janelia.it.jacs.model.common.SystemConfigurationProperties camProps;
//    private org.janelia.it.jacs.model.user_data.blast.BlastDatabaseFileNode blastDBFileNode;
//    private org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode blastResFileNode;
//    private org.janelia.it.jacs.model.user_data.FastaFileNode fastaFileNode;
//    private org.janelia.it.jacs.model.user_data.FileNode fileNode;
//    private org.janelia.it.jacs.model.common.ParameterVOMapUserType paramVoMapUserType;
//    private org.janelia.it.jacs.model.user_data.blast.BlastResultNode blastResNode;
//    private org.janelia.it.jacs.model.genomics.BlastHit blastHit;
    private org.janelia.it.jacs.model.collections.ImmutableHashMap imutHashmap;
    private org.janelia.it.jacs.model.collections.ImmutableHashSet imutHashset;
    private org.janelia.it.jacs.model.common.BlastableNodeVO bnVo;
    private org.janelia.it.jacs.model.download.Author author;
    private org.janelia.it.jacs.model.download.DataFile datafile;
    private org.janelia.it.jacs.model.download.HierarchyNode hierarchyNode;
    private org.janelia.it.jacs.model.download.Project project;
    private org.janelia.it.jacs.model.download.Publication publication;
    private org.janelia.it.jacs.model.genomics.Assembly assemble;
    private org.janelia.it.jacs.model.genomics.BaseSequenceEntity bseEntity;
    private org.janelia.it.jacs.model.genomics.NASequence naSequence;
    private org.janelia.it.jacs.model.genomics.AASequence aaSequence;
    private org.janelia.it.jacs.model.genomics.BioSequence biosequence;
    private org.janelia.it.jacs.model.genomics.Chromosome chromosome;
    private EntityTypeGenomic entitytype;
    private org.janelia.it.jacs.model.genomics.Peptide peptide;
    private org.janelia.it.jacs.model.genomics.Nucleotide nucleotide;
    private org.janelia.it.jacs.model.genomics.ORF orf;
    private org.janelia.it.jacs.model.genomics.Read read;
    private org.janelia.it.jacs.model.genomics.Scaffold scaffold;
    private org.janelia.it.jacs.model.genomics.SequenceException seqException;
    private org.janelia.it.jacs.model.genomics.SequenceType seqType;
    private org.janelia.it.jacs.model.genomics.SeqUtil seqUtil;
    private org.janelia.it.jacs.model.metadata.Library library;
    private org.janelia.it.jacs.model.metadata.GeoPath geoPath;
    private org.janelia.it.jacs.model.metadata.GeoPoint geoPoint;
    private org.janelia.it.jacs.model.metadata.Sample sample;
    private org.janelia.it.jacs.model.metadata.BioMaterial bioMaterial;
    private BlastNTask blastNTask;
    private BlastPTask blastPTask;
    private BlastTask blastTask;
    private BlastXTask blastXTask;
    private MegablastTask megaBlastTask;
    private org.janelia.it.jacs.model.tasks.Task task;
    private TBlastNTask tBlastNTask;
    private TBlastXTask tBlastXTask;
    private Blastable blastable;
    private org.janelia.it.jacs.model.user_data.DataSource dataSource;
    private org.janelia.it.jacs.model.tasks.Event event;
    private org.janelia.it.jacs.model.user_data.Node node;
    private org.janelia.it.jacs.model.user_data.User user;
    private org.janelia.it.jacs.model.vo.ParameterException parameterException;
    private org.janelia.it.jacs.model.vo.ParameterVO parameterVO;
    private org.janelia.it.jacs.model.vo.BooleanParameterVO boolParamVo;
    private org.janelia.it.jacs.model.vo.DoubleParameterVO doubleParamVo;
    private org.janelia.it.jacs.model.vo.LongParameterVO longParamVo;
    private org.janelia.it.jacs.model.vo.MultiSelectVO multiSelVo;
    private org.janelia.it.jacs.model.vo.SingleSelectVO singleSelectVo;
    private org.janelia.it.jacs.model.vo.TextParameterVO textParamVo;

    public void onModuleLoad() {
    }
}
