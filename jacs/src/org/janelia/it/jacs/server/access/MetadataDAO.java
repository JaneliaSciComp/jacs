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

package org.janelia.it.jacs.server.access;

import org.janelia.it.jacs.model.metadata.Library;
import org.janelia.it.jacs.model.metadata.Sample;
import org.janelia.it.jacs.server.access.hibernate.DaoException;

import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: Dec 28, 2006
 * Time: 9:47:55 AM
 */
public interface MetadataDAO extends DAO {

    public int getNumSamples() throws DaoException;

    /**
     * This method returns a list of Sample objects
     *
     * @param startIndex
     * @param numRows
     * @return list of samples
     * @throws DaoException
     */
    public List<Sample> getPagedSamples(int startIndex, int numRows) throws DaoException;

    /**
     * This method returns a list of Sample objects for the given libraryId
     *
     * @param libraryId
     * @return list of samples
     * @throws DaoException
     */
    public List<Sample> getSamplesByLibraryId(Long libraryId) throws DaoException;

    /**
     * This method returns a list of Sample objects with their sites loaded for the given libraryId
     *
     * @param libraryId
     * @return list of samples
     * @throws DaoException
     */
    public List<Sample> getSamplesWithSitesByLibraryId(Long libraryId) throws DaoException;

    /**
     * This method returns a list of Sample objects with their sites loaded for the given readId
     *
     * @param entityAcc
     * @return the first encountered sample associated with the entity
     * @throws DaoException
     */
    public Sample getSampleWithSitesByEntityAcc(String entityAcc) throws DaoException;

    /**
     * This method returns a Sample object with their sites loaded for the given sampleAcc.
     *
     * @param sampleAcc
     * @return the sample with the given accession
     * @throws DaoException
     */
    public Sample getSampleWithSitesBySampleAcc(String sampleAcc) throws DaoException;

    /**
     * This method returns a Sample object with their sites loaded for the given sample name
     *
     * @param sampleName
     * @return the first encountered sample associated with the entity
     * @throws DaoException
     */
    public Sample getSampleWithSitesBySampleName(String sampleName) throws DaoException;

    /**
     * This method returns a Library given a Read entityId
     *
     * @param readEntityId
     * @return Library for the given Read entityId
     * @throws DaoException
     */
    public Library getLibraryByReadEntityId(Long readEntityId) throws DaoException;
}
