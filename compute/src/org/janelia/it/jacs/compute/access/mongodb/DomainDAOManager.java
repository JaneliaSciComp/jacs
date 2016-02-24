package org.janelia.it.jacs.compute.access.mongodb;

import java.net.UnknownHostException;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.domain.support.DomainDAO;

public class DomainDAOManager {

    private static final Logger logger = Logger.getLogger(DomainDAOManager.class);
    
    private static String MONGO_SERVER_URL = SystemConfigurationProperties.getString("MongoDB.ServerURL");
    private static String MONGO_DATABASE = SystemConfigurationProperties.getString("MongoDB.Database");
    private static String MONGO_USERNAME = SystemConfigurationProperties.getString("MongoDB.Username");
    private static String MONGO_PASSWORD = SystemConfigurationProperties.getString("MongoDB.Password");
    
    private static DomainDAOManager instance;

    protected DomainDAO dao;
    
    private DomainDAOManager() {
        try {
            this.dao = new DomainDAO(MONGO_SERVER_URL, MONGO_DATABASE, MONGO_USERNAME, MONGO_PASSWORD);
        }
        catch (UnknownHostException e) {
            logger.error("Error connecting to Mongo",e);
        }
    }
    
    public static DomainDAOManager getInstance() {
        if (instance==null) {
            instance = new DomainDAOManager();
        }
        return instance;
    }

    public DomainDAO getDao() {
        return dao;
    }
}
