package org.janelia.it.jacs.compute.mongo;

import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.junit.BeforeClass;

public class MongoDbTest {

    /* TODO: move these into a test.properties file */
    protected static final String MONGO_SERVER_URL = "mongo-db";
    protected static final String MONGO_DATABASE = "jacs";
    protected static final String MONGO_USERNAME = "flyportal";
    protected static final String MONGO_PASSWORD = "flyportal";
    
    protected static DomainDAO dao;
    
    @BeforeClass
    public static void init() throws Exception {
        dao = new DomainDAO(MONGO_SERVER_URL, MONGO_DATABASE, MONGO_USERNAME, MONGO_PASSWORD);
    }
}
