package org.janelia.it.jacs.model.domain;

import java.net.UnknownHostException;

import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.jongo.marshall.jackson.JacksonMapper;
import org.junit.BeforeClass;

import com.fasterxml.jackson.databind.MapperFeature;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;

public class MongoDbTest {

    protected static final String MONGO_SERVER_URL = "rokicki-ws";
    protected static final String MONGO_DATABASE = "jacs";
    
    protected static MongoClient m;
    protected static DB db;
    protected static MongoCollection  folderCollection;
    protected static MongoCollection  sampleCollection;
    protected static Jongo jongo;
    
    @BeforeClass
    public static void init() throws Exception {
        try {
            m = new MongoClient(MONGO_SERVER_URL);
            m.setWriteConcern(WriteConcern.UNACKNOWLEDGED);
            db = m.getDB(MONGO_DATABASE);
            jongo = new Jongo(db, 
                    new JacksonMapper.Builder()
                        .enable(MapperFeature.AUTO_DETECT_GETTERS)
                        .enable(MapperFeature.AUTO_DETECT_SETTERS)
                        .build());
            folderCollection = jongo.getCollection("folder").withWriteConcern(WriteConcern.UNACKNOWLEDGED);
            sampleCollection = jongo.getCollection("sample").withWriteConcern(WriteConcern.UNACKNOWLEDGED);
        }
        catch (UnknownHostException e) {
            throw new RuntimeException("Unknown host given in MongoDB.ServerURL value in system properties: "+MONGO_SERVER_URL);
        }
    }
}
