
package org.janelia.it.jacs.web.gwt.admin.server;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.janelia.it.jacs.server.access.hibernate.UserDAOImpl;
import org.janelia.it.jacs.server.api.DataSetAPI;
import org.janelia.it.jacs.server.api.FeatureAPI;
import org.janelia.it.jacs.server.api.UserAPI;
import org.janelia.it.jacs.web.gwt.admin.client.service.AdminService;
import org.janelia.it.jacs.web.gwt.common.server.JcviGWTSpringController;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Nov 2, 2006
 * Time: 3:52:23 PM
 */
public class AdminServiceImpl extends JcviGWTSpringController implements AdminService {

    private UserAPI userAPI = new UserAPI();
    private FeatureAPI featureAPI = new FeatureAPI();
    private DataSetAPI dataSetAPI = new DataSetAPI();
    private UserDAOImpl userDAO;
    private static Logger logger = Logger.getLogger(AdminServiceImpl.class);

    public void setUserDAO(UserDAOImpl userDAO) {
        this.userDAO = userDAO;
    }

    public DataSetAPI getDataSetAPI() {
        return dataSetAPI;
    }

    public void setDataSetAPI(DataSetAPI dataSetAPI) {
        this.dataSetAPI = dataSetAPI;
    }

    public FeatureAPI getFeatureAPI() {
        return featureAPI;
    }

    public void setFeatureAPI(FeatureAPI featureAPI) {
        this.featureAPI = featureAPI;
    }

    public UserAPI getUserAPI() {
        return userAPI;
    }

    public void setUserAPI(UserAPI userAPI) {
        this.userAPI = userAPI;
    }

    public String createUser(String login, String name) {
        // A call to the db to get the user, per page transition, may be a little too expensive.
        // Create the user if need be
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Getting user object: " + login);
            }
            User user = userDAO.getUserByName(login);
            if (null == user) {
                logger.info("Creating user " + login);
                user = userDAO.createUser(login, name);
                return "Creation of user " + login + " succeeded.";
            }
            return "User " + login + " already exists.";
        }
        catch (DaoException e) {
            logger.error("DAOException: " + e.getMessage(), e);
            e.printStackTrace();
        }
        catch (Throwable e) {
            logger.error("Throwable: " + e.getMessage(), e);
        }
        return "Creation of user " + login + " failed.";
    }

    public ArrayList<String> getDiskUsageReport() {
        Scanner scanner = null;
        try {
            ArrayList<DiskInfo> diskUsage = new ArrayList<DiskInfo>();
            scanner = new Scanner(new File(SystemConfigurationProperties.getString("Reports.Dir") + File.separator + SystemConfigurationProperties.getString("DiskUsageFilename")));
            while (scanner.hasNextLine()) {
                String[] pieces = scanner.nextLine().split("\t");
                String path = pieces[1].substring("Xruntime-shared/filestore/".length());
                if (path.indexOf("/") < 0) {
                    diskUsage.add(new DiskInfo(new Long(pieces[0]), path));
                }
            }
            Collections.sort(diskUsage);
            ArrayList<String> returnInfo = new ArrayList<String>();
            NumberFormat format = new DecimalFormat("0.00");

            for (DiskInfo o : diskUsage) {
                String tmpSize = format.format(o.getSize() / 1000.0);
                returnInfo.add(tmpSize + "\t" + o.getUser());
            }
            return returnInfo;
        }
        catch (FileNotFoundException e) {
            logger.error("Throwable: " + e.getMessage(), e);
        }
        finally {
            if (null != scanner) {
                scanner.close();
            }
        }
        return null;
    }

    private class DiskInfo implements Comparable {
        private Long size;
        private String user;

        private DiskInfo(Long size, String user) {
            this.size = size;
            this.user = user;
        }

        @Override
        // Sort by size
        public int compareTo(Object o) {
            return ((DiskInfo) o).getSize().compareTo(this.getSize());
        }

        public Long getSize() {
            return size;
        }

        public String getUser() {
            return user;
        }
    }
}