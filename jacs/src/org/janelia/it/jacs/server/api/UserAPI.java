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

package org.janelia.it.jacs.server.api;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.common.UserDataVO;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.janelia.it.jacs.server.access.hibernate.UserDAOImpl;
import org.janelia.it.jacs.server.utils.SystemException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Aug 25, 2006
 * Time: 11:47:43 AM
 */
public class UserAPI {
    static Logger logger = Logger.getLogger(UserAPI.class.getName());
    UserDAOImpl userDAO;

    public void setUserDAO(UserDAOImpl userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * The method searches the FastaFileNode(s) for the <code>user</code>
     *
     * @param searchString
     * @param startIndex
     * @param numRows
     * @param sortArgs
     * @return
     * @throws SystemException
     */
    public UserDataVO[] getPagedUsers(String searchString, int startIndex, int numRows, SortArgument[] sortArgs)
            throws SystemException {
        try {
            logger.debug(numRows + " requested");
            int dbStartIndex = startIndex;
            int dbNumRows = numRows;
            SortArgument[] actualSortArgs = null;
            if (sortArgs != null) {
                actualSortArgs = new SortArgument[sortArgs.length];
                for (int i = 0; i < sortArgs.length; i++) {
                    String sortField = sortArgs[i].getSortArgumentName();
                    // todo Remove this hard-wiring;  Strings from the front-end were ALL SCREWED UP (full URLs streamed in)
                    sortField = UserDataVO.SORT_BY_USER_LOGIN;
                    if (sortField == null || sortField.length() == 0) {
                        continue;
                    }
                    if (sortField.equals(UserDataVO.SORT_BY_USER_LOGIN)) {
                        sortField = UserDataVO.SORT_BY_USER_LOGIN;
                    }
                    else if (sortField.equals(UserDataVO.SORT_BY_USER_ID)) {
                        sortField = UserDataVO.SORT_BY_USER_ID;
                    }
                    else if (sortField.equals(UserDataVO.SORT_BY_FULLNAME)) {
                        sortField = UserDataVO.SORT_BY_FULLNAME;
                    }
                    else if (sortField.equals(UserDataVO.SORT_BY_EMAIL)) {
                        sortField = UserDataVO.SORT_BY_EMAIL;
                    }
                    else {
                        // unknown or unsupported sort field name
                        continue;
                    }
                    actualSortArgs[i] = new SortArgument(sortField, sortArgs[i].getSortDirection());
                }
            }
            List<User> userList = userDAO.getPagedUsers(searchString, dbStartIndex, dbNumRows, actualSortArgs);
            logger.debug("received " + userList.size() + " rows from userDAO.getPagedUsers");
            ArrayList<UserDataVO> voList = new ArrayList<UserDataVO>();
            for (Object anUserList : userList) {
                User user = (User) anUserList;
                UserDataVO uVo = new UserDataVO(
                        user.getUserLogin() + "",
                        user.getUserId(),
                        user.getFullName(),
                        user.getEmail());
                voList.add(uVo);
            }
            UserDataVO[] voArr = voList.toArray(new UserDataVO[voList.size()]);
            logger.debug("returning voArr with " + voArr.length + " entires");
            return voArr;
        }
        catch (DaoException e) {
            throw new SystemException(e);
        }
    }


    public void markUserForDeletion(String userId) throws SystemException {
        throw new SystemException("Currently unable to delete user " + userId);
    }

    public Integer getNumUsers(String searchString) throws SystemException {
        try {
            return userDAO.getNumUsers(searchString);
        }
        catch (DaoException e) {
            throw new SystemException(e);
        }
    }

}
