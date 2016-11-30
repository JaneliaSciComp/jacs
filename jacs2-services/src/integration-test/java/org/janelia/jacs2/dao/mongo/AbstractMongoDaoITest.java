package org.janelia.jacs2.dao.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.janelia.jacs2.cdi.ObjectMapperFactory;
import org.janelia.jacs2.dao.Dao;
import org.janelia.jacs2.model.domain.sample.Sample;
import org.janelia.jacs2.utils.DomainCodecProvider;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AbstractMongoDaoITest<T, I> {

    private static MongoClient testMongoClient;
    private static ObjectMapper testObjectMapper = ObjectMapperFactory.instance().getObjectMapper();

    private static Properties testConfig;

    protected MongoDatabase testMongoDatabase;

    @BeforeClass
    public static void setUpMongoClient() throws IOException {
        testConfig = new Properties();
        try (InputStream configReader = new FileInputStream("build/resources/integrationTest/jacs_test.properties")) {
            testConfig.load(configReader);
        }
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
                MongoClient.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(new DomainCodecProvider(testObjectMapper))
        );
        MongoClientOptions.Builder optionsBuilder = MongoClientOptions.builder().codecRegistry(codecRegistry);
        MongoClientURI mongoConnectionString = new MongoClientURI(testConfig.getProperty("MongoDB.ConnectionURL"), optionsBuilder);
        testMongoClient = new MongoClient(mongoConnectionString);
    }

    @Before
    public final void setUpMongoDatabase() {
        testMongoDatabase = testMongoClient.getDatabase(testConfig.getProperty("MongoDB.Database"));
    }

    protected void delete(Dao<T, I> dao, T e) {
        dao.delete(e);
    }

}
