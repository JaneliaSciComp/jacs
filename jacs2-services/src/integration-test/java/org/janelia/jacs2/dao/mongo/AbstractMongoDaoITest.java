package org.janelia.jacs2.dao.mongo;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistry;
import org.janelia.jacs2.AbstractITest;
import org.janelia.jacs2.cdi.ObjectMapperFactory;
import org.janelia.jacs2.dao.ReadWriteDao;
import org.janelia.it.jacs.model.domain.interfaces.HasIdentifier;
import org.janelia.jacs2.dao.mongo.utils.RegistryHelper;
import org.janelia.jacs2.dao.mongo.utils.TimebasedIdentifierGenerator;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.IOException;
import java.util.List;
import java.util.Random;

public abstract class AbstractMongoDaoITest<T extends HasIdentifier> extends AbstractITest {
    private static MongoClient testMongoClient;
    protected static ObjectMapperFactory testObjectMapperFactory = ObjectMapperFactory.instance();

    protected MongoDatabase testMongoDatabase;
    protected TimebasedIdentifierGenerator idGenerator = new TimebasedIdentifierGenerator(0);
    protected Random dataGenerator = new Random();

    @BeforeClass
    public static void setUpMongoClient() throws IOException {
        CodecRegistry codecRegistry = RegistryHelper.createCodecRegistry(testObjectMapperFactory);
        MongoClientOptions.Builder optionsBuilder = MongoClientOptions.builder().codecRegistry(codecRegistry).maxConnectionIdleTime(60000);
        MongoClientURI mongoConnectionString = new MongoClientURI(integrationTestsConfig.getProperty("MongoDB.ConnectionURL"), optionsBuilder);
        testMongoClient = new MongoClient(mongoConnectionString);
    }

    @Before
    public final void setUpMongoDatabase() {
        testMongoDatabase = testMongoClient.getDatabase(integrationTestsConfig.getProperty("MongoDB.Database"));
    }

    protected void deleteAll(ReadWriteDao<T, Number> dao, List<T> es) {
        for (T e : es) {
            delete(dao, e);
        }
    }

    protected void delete(ReadWriteDao<T, Number> dao, T e) {
        if (e.getId() != null) {
            dao.delete(e);
        }
    }

    protected abstract List<T> createMultipleTestItems(int nItems);

}
