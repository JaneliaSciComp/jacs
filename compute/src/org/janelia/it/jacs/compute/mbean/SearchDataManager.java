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

package org.janelia.it.jacs.compute.mbean;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.shared.lucene.LuceneIndexer;

import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jul 2, 2007
 * Time: 5:00:41 PM
 */
public class SearchDataManager implements SearchDataManagerMBean {

    private static final Logger LOGGER = Logger.getLogger(SearchDataManager.class);

    public SearchDataManager() {
    }

    public void updateAllIndices() {
        updateIndexForData(LuceneIndexer.INDEX_ALL);
    }

    public void updateIndexForClusterData() {
        updateIndexForData(LuceneIndexer.INDEX_CLUSTERS);
    }

    public void updateIndexForProjectData() {
        updateIndexForData(LuceneIndexer.INDEX_PROJECTS);
    }

    public void updateIndexForProteinData() {
        updateIndexForData(LuceneIndexer.INDEX_PROTEINS);
    }

    public void updateIndexForPublicationData() {
        updateIndexForData(LuceneIndexer.INDEX_PUBLICATIONS);
    }

    public void updateIndexForSampleData() {
        updateIndexForData(LuceneIndexer.INDEX_SAMPLES);
    }

    private void updateIndexForData(String dataType) {
        LOGGER.debug("Updating Lucene index file for data type=" + dataType);
        try {
            LuceneIndexer indexer = new LuceneIndexer();
            Set<String> tmpDocTypeSet;
            if (LuceneIndexer.INDEX_ALL.equals(dataType)) {
                tmpDocTypeSet = LuceneIndexer.SET_OF_ALL_DOC_TYPES;
            }
            else {
                tmpDocTypeSet = new HashSet<String>();
                tmpDocTypeSet.add(dataType);
            }
            indexer.execute(tmpDocTypeSet, Integer.MAX_VALUE);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Search Methods
     */
    public void searchPublication(String searchString) {
        try {
            EJBFactory.getRemoteSearchBean().search(LuceneIndexer.INDEX_PUBLICATIONS, searchString);
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void searchProject(String searchString) {
        try {
            EJBFactory.getRemoteSearchBean().search(LuceneIndexer.INDEX_PROJECTS, searchString);
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void searchFinalCluster(String searchString) {
        try {
            EJBFactory.getRemoteSearchBean().search(LuceneIndexer.INDEX_CLUSTERS, searchString);
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void searchProtein(String searchString) {
        try {
            EJBFactory.getRemoteSearchBean().search(LuceneIndexer.INDEX_PROTEINS, searchString);
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void searchSamples(String searchString) {
        try {
            EJBFactory.getRemoteSearchBean().search(LuceneIndexer.INDEX_SAMPLES, searchString);
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}