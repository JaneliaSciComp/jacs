package org.janelia.it.jacs.compute.access.mongodb;

import java.net.UnknownHostException;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.jongo.MongoCollection;

/**
 * Data maintenance for the MongoDB data store. For example, denormalization operations. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MongoDbMaintainer {

    private static final Logger log = Logger.getLogger(MongoDbMaintainer.class);

    protected DomainDAO dao;

    public MongoDbMaintainer() throws UnknownHostException {
        this.dao = DomainDAOManager.getInstance().getDao();
    }

    public void refreshPermissions() throws DaoException {

        long start = System.currentTimeMillis();
        log.info("Refreshing denormalized permissions");

        Multimap<String, String> groupMap = HashMultimap.create();
        for (Subject subject : dao.getCollectionByName("subject").find().as(Subject.class)) {
            for (String groupKey : subject.getGroups()) {
                groupMap.put(groupKey, subject.getKey());
            }
        }

        Set<String> collectionNames = DomainUtils.getCollectionNames();
        for (String collectionName : collectionNames) {
            log.info("Refreshing denormalized permissions for " + collectionName);

            MongoCollection collection = dao.getCollectionByName(collectionName);
            Class<?> domainClass = DomainUtils.getBaseClass(collectionName);
            if (!DomainObject.class.isAssignableFrom(domainClass))
                continue;
            Iterable<?> iterable = collection.find().as(domainClass);

            if (iterable == null) {
                log.info("Could not iterate collection " + collectionName + " as " + domainClass);
                continue;
            }

            for (Object obj : iterable) {
                DomainObject domainObject = (DomainObject) obj;
                String ownerKey = domainObject.getOwnerKey();
                if (ownerKey == null)
                    continue;
                collection.update("{_id:#}", domainObject.getId()).with("{$addToSet:{readers:#,writers:#}}", ownerKey, ownerKey);

            }
        }

        log.info("Refreshing permissions took " + (System.currentTimeMillis() - start) + " ms");
    }

    public void ensureIndexes() throws DaoException {

        long start = System.currentTimeMillis();
        log.info("Ensuring indexes");

        // Core Model (Shared)

        MongoCollection subjectCollection = dao.getCollectionByName("subject");
        subjectCollection.ensureIndex("{key:1}","{unique:true}");
        subjectCollection.ensureIndex("{name:1}");
        subjectCollection.ensureIndex("{groups:1}");

        MongoCollection treeNodeCollection = dao.getCollectionByName("treeNode");
        ensureDomainIndexes(treeNodeCollection);

        MongoCollection ontologyCollection = dao.getCollectionByName("ontology");
        ensureDomainIndexes(ontologyCollection);

        MongoCollection annotationCollection = dao.getCollectionByName("annotation");
        ensureDomainIndexes(annotationCollection);
        annotationCollection.ensureIndex("{target:1}");
        annotationCollection.ensureIndex("{target:1,readers:1}");

        // Fly Model

        MongoCollection alignmentBoardCollection = dao.getCollectionByName("alignmentBoard");
        ensureDomainIndexes(alignmentBoardCollection);

        MongoCollection compartmentSetCollection = dao.getCollectionByName("compartmentSet");
        ensureDomainIndexes(compartmentSetCollection);

        MongoCollection dataSetCollection = dao.getCollectionByName("dataSet");
        ensureDomainIndexes(dataSetCollection);
        dataSetCollection.ensureIndex("{identifier:1}","{unique:true}");
        dataSetCollection.ensureIndex("{pipelineProcesses:1}");

        MongoCollection flyLineCollection = dao.getCollectionByName("flyLine");
        ensureDomainIndexes(flyLineCollection);
        flyLineCollection.ensureIndex("{robotId:1}");
        flyLineCollection.ensureIndex("{balancedLine:1,readers:1}");
        flyLineCollection.ensureIndex("{originalLine:1,readers:1}");
        flyLineCollection.ensureIndex("{representativeScreenSample:1,readers:1}");

        MongoCollection fragmentCollection = dao.getCollectionByName("fragment");
        ensureDomainIndexes(fragmentCollection);
        fragmentCollection.ensureIndex("{separationId:1}");
        fragmentCollection.ensureIndex("{separationId:1,readers:1}");
        fragmentCollection.ensureIndex("{sampleRef:1}");
        fragmentCollection.ensureIndex("{sampleRef:1,readers:1}");

        MongoCollection imageCollection = dao.getCollectionByName("image");
        ensureDomainIndexes(imageCollection);
        imageCollection.ensureIndex("{sageId:1}");
        imageCollection.ensureIndex("{slideCode:1}");
        imageCollection.ensureIndex("{filepath:1}");
        imageCollection.ensureIndex("{sampleRef:1}");
        imageCollection.ensureIndex("{sampleRef:1,readers:1}");

        MongoCollection sampleCollection = dao.getCollectionByName("sample");
        ensureDomainIndexes(sampleCollection);
        sampleCollection.ensureIndex("{dataSet:1}");

        // Mouse Model

        MongoCollection tmSampleCollection = dao.getCollectionByName("tmSample");
        ensureDomainIndexes(tmSampleCollection);

        MongoCollection tmWorkspaceCollection = dao.getCollectionByName("tmWorkspace");
        ensureDomainIndexes(tmWorkspaceCollection);
        subjectCollection.ensureIndex("{sampleRef:1}");
        subjectCollection.ensureIndex("{sampleRef:1,readers:1}");


        log.info("Indexing MongoDB took " + (System.currentTimeMillis() - start) + " ms");
    }

    private void ensureDomainIndexes(MongoCollection mc) {
        mc.ensureIndex("{ownerKey:1}");
        mc.ensureIndex("{writers:1}");
        mc.ensureIndex("{readers:1}");
        mc.ensureIndex("{name:1}");
        mc.ensureIndex("{_id:1,writers:1}");
        mc.ensureIndex("{_id:1,readers:1}");
    }

    /**
     * Test harness
     */
    public static void main(String[] args) throws Exception {
        MongoDbMaintainer refresh = new MongoDbMaintainer();
        refresh.refreshPermissions();
        refresh.ensureIndexes();
    }
}
