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

package org.janelia.it.jacs.server.access.hibernate;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.janelia.it.jacs.model.common.SortArgument;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Aug 30, 2006
 * Time: 11:50:57 AM
 */
public abstract class DaoBaseImpl extends HibernateDaoSupport {
    protected static Logger logger = Logger.getLogger(DaoBaseImpl.class);

    protected String buildOrderByClause(SortArgument[] sortArgs) {
        StringBuffer orderByFieldsBuffer = new StringBuffer();
        if (sortArgs != null) {
            for (SortArgument sortArg : sortArgs) {
                String dataSortField = sortArg.getSortArgumentName();
                if (dataSortField == null || dataSortField.length() == 0) {
                    continue;
                }
                if (sortArg.isAsc()) {
                    if (orderByFieldsBuffer.length() > 0) {
                        orderByFieldsBuffer.append(',');
                    }
                    orderByFieldsBuffer.append(dataSortField + " asc");
                }
                else if (sortArg.isDesc()) {
                    if (orderByFieldsBuffer.length() > 0) {
                        orderByFieldsBuffer.append(',');
                    }
                    orderByFieldsBuffer.append(dataSortField + " desc");
                }
            } // end for all sortArgs
        }
        String orderByClause = "";
        if (orderByFieldsBuffer.length() > 0) {
            orderByClause = "order by " + orderByFieldsBuffer.toString();
        }
        return orderByClause;
    }

    protected Object genericGet(Class c, Long id) throws DaoException {
        try {
            HibernateTemplate ht = getHibernateTemplate();
            return ht.get(c, id);
        }
        catch (Exception e) {
            throw new DaoException(e, "Get " + c.getName() + "->" + id);
        }
    }

    protected void saveOrUpdateObject(Object obj, String source) throws DataAccessException, DaoException {
        try {
            HibernateTemplate ht = getHibernateTemplate();
            ht.saveOrUpdate(obj);
            // force save to the DB now instead of waiting for session to finish
            ht.flush();
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, source);
        }
        catch (IllegalStateException e) {
            throw handleException(e, source);
        }
    }

    protected DaoException handleThrowable(Throwable t, String actionWhichProducedError) {
        logger.error(t);
        return new DaoException(actionWhichProducedError);
    }

    protected DaoException handleException(Exception e, String actionWhichProducedError) {
        logger.error(e);
        return new DaoException(e, actionWhichProducedError);
    }

    protected DataAccessException handleException(HibernateException e, String actionWhichProducedError) {
        logger.error(e);
        return convertHibernateAccessException(e);
    }

    /**
     * This method can be used by subclasses to avoid checking for list size before calling list.get(0)
     *
     * @param hqlQueryName
     * @param paramName
     * @param paramValue
     * @param uniqueResult
     * @return list containing query result
     */
    protected Object findByNamedQueryAndNamedParam(String hqlQueryName, String paramName, Object paramValue, boolean uniqueResult) {
        List result = getHibernateTemplate().findByNamedQueryAndNamedParam(hqlQueryName, paramName, paramValue);
        if (result == null) {
            return null;
        }
        if (uniqueResult) {
            if (result.size() == 0) {
                return null;
            }
            if (result.size() != 1) {
                //throw exception
            }
            return result.get(0);
        }
        else {
            return result;
        }
    }

}
