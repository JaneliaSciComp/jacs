package org.janelia.it.jacs.compute.mongo;

import org.janelia.it.jacs.compute.access.mongodb.DomainDAO;
import org.junit.BeforeClass;

public class MongoDbTest {

    /* TODO: move these into a test.properties file */
    protected static final String MONGO_SERVER_URL = "rokicki-ws";
    protected static final String MONGO_DATABASE = "jacs";
    
    protected static DomainDAO dao;
    
    @BeforeClass
    public static void init() throws Exception {
        dao = new DomainDAO(MONGO_SERVER_URL, MONGO_DATABASE);
    }
}
