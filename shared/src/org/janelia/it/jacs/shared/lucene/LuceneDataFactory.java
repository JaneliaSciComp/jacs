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

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Mar 5, 2008
 * Time: 6:00:46 PM
 *
 */
package org.janelia.it.jacs.shared.lucene;

import org.janelia.it.jacs.shared.lucene.data_retrievers.*;
import org.janelia.it.jacs.shared.lucene.searchers.*;

import java.io.IOException;

public class LuceneDataFactory {

    public static LuceneDataRetrieverBase createDocumentRetriever(String docType) {
        if (LuceneIndexer.SET_OF_ALL_DOC_TYPES.contains(docType)) {
            if (docType.equals(LuceneIndexer.INDEX_CLUSTERS)) {
                return new ClustersDataRetriever();
            }
            else if (docType.equals(LuceneIndexer.INDEX_PROJECTS)) {
                return new ProjectsDataRetriever();
            }
            else if (docType.equals(LuceneIndexer.INDEX_PROTEINS)) {
                return new ProteinsDataRetriever();
            }
            else if (docType.equals(LuceneIndexer.INDEX_PUBLICATIONS)) {
                return new PublicationsDataRetriever();
            }
            else if (docType.equals(LuceneIndexer.INDEX_SAMPLES)) {
                return new SamplesDataRetriever();
            }
        }

        return null;
    }

    public static LuceneSearcherBase getDocumentSearcher(String docType) throws IOException {
        if (LuceneIndexer.SET_OF_ALL_DOC_TYPES.contains(docType)) {
            if (LuceneIndexer.INDEX_CLUSTERS.equals(docType)) {
                return new ClusterSearcher();
            }
            else if (LuceneIndexer.INDEX_PROJECTS.equals(docType)) {
                return new ProjectsSearcher();
            }
            else if (LuceneIndexer.INDEX_PROTEINS.equals(docType)) {
                return new ProteinSearcher();
            }
            else if (LuceneIndexer.INDEX_PUBLICATIONS.equals(docType)) {
                return new PublicationSearcher();
            }
            else if (LuceneIndexer.INDEX_SAMPLES.equals(docType)) {
                return new SampleSearcher();
            }
        }

        return null;
    }

}
