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

package org.janelia.it.jacs.compute.access;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.janelia.it.jacs.model.metadata.Sample;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jul 11, 2008
 * Time: 4:46:28 PM
 */
public class MetadataDAO extends ComputeBaseDAO {

    public MetadataDAO(Logger logger) {
        super(logger);
    }

    /**
     * This method returns a Sample object with their sites loaded for the given sampleAcc.
     *
     * @param sampleAcc
     * @return the sample with the given accession
     * @throws DaoException
     */
    public Sample getSampleWithSitesBySampleAcc(String sampleAcc) throws DaoException {
        try {
            Query query = getSession().createQuery("select sample from Sample sample where sample.sampleAcc = '" + sampleAcc + "'");
            List samples = query.list();
            return samples != null && samples.size() > 0 ? (Sample) samples.get(0) : null;
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, this.getClass().getName() + " - getSamples");
        }
    }

//    protected Object findByNamedQueryAndNamedParam(String hqlQueryName, String paramName, Object paramValue, boolean uniqueResult) {


}
