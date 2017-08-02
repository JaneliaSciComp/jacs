package org.janelia.jacs2.dao.mongo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.conversions.Bson;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.hamcrest.beans.HasPropertyWithValue;
import org.janelia.jacs2.dao.JacsServiceDataDao;
import org.janelia.jacs2.model.DataInterval;
import org.janelia.jacs2.model.SetFieldValueHandler;
import org.janelia.jacs2.model.jacsservice.JacsServiceLifecycleStage;
import org.janelia.jacs2.model.jacsservice.RegisteredJacsNotification;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.jacsservice.JacsServiceEvent;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;
import org.janelia.jacs2.model.jacsservice.ProcessingLocation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class JacsServiceDataMongoDaoITest extends AbstractMongoDaoITest<JacsServiceData> {

    private List<JacsServiceData> testData = new ArrayList<>();
    private JacsServiceDataDao testDao;
    private JacsServiceDataDao archiveDao;

    @Before
    public void setUp() {
        testDao = new JacsServiceDataMongoDao(testMongoDatabase, idGenerator);
        archiveDao = new JacsServiceDataMongoDao(testMongoDatabase, idGenerator) {
            @Override
            protected Class<JacsServiceData> getEntityType() {
                return JacsServiceData.class;
            }

            @Override
            public void delete(JacsServiceData entity) {
                super.delete(entity);
                archiveMongoCollection.deleteOne(eq("_id", entity.getId()));
            }
        };
    }

    @After
    public void tearDown() {
        // delete the data that was created for testing
        deleteAll(testDao, testData);
        deleteAll(archiveDao, testData);
    }

    @Test
    public void findByNullId() {
        assertNull(testDao.findById(null));
    }

    @Test
    public void findHierarchyByNullId() {
        assertNull(testDao.findServiceHierarchy(null));
    }

    @Test
    public void persistServiceData() {
        JacsServiceData si = persistServiceWithEvents(createTestService("s", ProcessingLocation.LOCAL),
                ImmutableMap.of(
                        "s1", new RegisteredJacsNotification().addNotificationField("nf1", "nv1").withDefaultLifecycleStages(),
                        "s2", new RegisteredJacsNotification().addNotificationField("nf2", "nv2").forLifecycleStage(JacsServiceLifecycleStage.FAILED_PROCESSING)
                ),
                createTestServiceEvent("e1", "v1"),
                createTestServiceEvent("e2", "v2"));
        JacsServiceData retrievedSi = testDao.findById(si.getId());
        assertThat(retrievedSi.getName(), equalTo(si.getName()));
    }

    @Test
    public void addServiceEvent() {
        JacsServiceData si = persistServiceWithEvents(createTestService("s", ProcessingLocation.LOCAL),
                ImmutableMap.of(
                        "s1", new RegisteredJacsNotification().addNotificationField("nf1", "nv1").withDefaultLifecycleStages(),
                        "s2", new RegisteredJacsNotification().addNotificationField("nf2", "nv2").forLifecycleStage(JacsServiceLifecycleStage.FAILED_PROCESSING)
                ),
                createTestServiceEvent("e1", "v1"),
                createTestServiceEvent("e2", "v2"));
        testDao.addServiceEvent(si, createTestServiceEvent("e3", "v3"));
        testDao.addServiceEvent(si, createTestServiceEvent("e4", "v4"));
        testDao.update(si, ImmutableMap.of("state", new SetFieldValueHandler<>(JacsServiceState.RUNNING)));
        JacsServiceData retrievedSi = testDao.findById(si.getId());
        assertThat(retrievedSi.getName(), equalTo(si.getName()));
        assertThat(retrievedSi.getEvents(),
                contains(new HasPropertyWithValue<>("name", CoreMatchers.equalTo("e1")),
                        new HasPropertyWithValue<>("name", CoreMatchers.equalTo("e2")),
                        new HasPropertyWithValue<>("name", CoreMatchers.equalTo("e3")),
                        new HasPropertyWithValue<>("name", CoreMatchers.equalTo("e4"))
                ));
    }

    @Test
    public void persistServiceHierarchyOneAtATime() {
        JacsServiceData si1 = persistServiceWithEvents(createTestService("s1", ProcessingLocation.LOCAL),
                ImmutableMap.of(
                        "s1", new RegisteredJacsNotification().addNotificationField("nf1", "nv1"),
                        "s2", new RegisteredJacsNotification().addNotificationField("nf2", "nv2")
                ));
        JacsServiceData retrievedSi1 = testDao.findById(si1.getId());
        assertThat(retrievedSi1, allOf(
                hasProperty("parentServiceId", nullValue(Long.class)),
                hasProperty("rootServiceId", nullValue(Long.class))
        ));
        JacsServiceData si1_1 = createTestService("s1.1", ProcessingLocation.LOCAL);
        si1_1.updateParentService(si1);
        testDao.save(si1_1);
        JacsServiceData retrievedSi1_1 = testDao.findById(si1_1.getId());
        assertThat(retrievedSi1_1, allOf(
                hasProperty("parentServiceId", equalTo(si1.getId())),
                hasProperty("rootServiceId", equalTo(si1.getId()))
        ));

        JacsServiceData si1_2 = createTestService("s1.2", ProcessingLocation.LOCAL);
        si1_2.updateParentService(si1);
        testDao.save(si1_2);
        JacsServiceData retrievedSi1_2 = testDao.findById(si1_2.getId());
        assertThat(retrievedSi1_2, allOf(
                hasProperty("parentServiceId", equalTo(si1.getId())),
                hasProperty("rootServiceId", equalTo(si1.getId()))
        ));

        JacsServiceData si1_2_1 = createTestService("s1.2.1", ProcessingLocation.LOCAL);
        si1_2_1.updateParentService(si1_2);
        testDao.save(si1_2_1);

        JacsServiceData retrievedSi1_2_1 = testDao.findById(si1_2_1.getId());
        assertThat(retrievedSi1_2_1, allOf(
                hasProperty("parentServiceId", equalTo(si1_2.getId())),
                hasProperty("rootServiceId", equalTo(si1.getId()))
        ));

        List<JacsServiceData> s1Children = testDao.findChildServices(si1.getId());
        assertThat(s1Children.size(), equalTo(2));
        assertThat(s1Children, everyItem(Matchers.hasProperty("parentServiceId", equalTo(si1.getId()))));

        List<JacsServiceData> s1Hierarchy = testDao.findServiceHierarchy(si1.getId()).serviceHierarchyStream().collect(Collectors.toList());
        assertThat(s1Hierarchy.size(), equalTo(4));
        assertThat(s1Hierarchy.subList(1, s1Hierarchy.size()), everyItem(Matchers.hasProperty("rootServiceId", equalTo(si1.getId()))));
        assertThat(s1Hierarchy.get(0), Matchers.hasProperty("rootServiceId", Matchers.nullValue()));
    }

    @Test
    public void persistServiceHierarchyAllAtOnce() {
        JacsServiceData si1 = createTestService("s1", ProcessingLocation.LOCAL);
        JacsServiceData si1_1 = createTestService("s1.1", ProcessingLocation.LOCAL);
        JacsServiceData si1_2 = createTestService("s1.2", ProcessingLocation.LOCAL);
        JacsServiceData si1_3 = createTestService("s1.3", ProcessingLocation.LOCAL);
        JacsServiceData si1_2_1 = createTestService("s1.2.1", ProcessingLocation.LOCAL);
        si1.addServiceDependency(si1_1);
        si1.addServiceDependency(si1_2);
        si1_1.addServiceDependency(si1_2);
        si1_1.addServiceDependency(si1_3);
        si1_2.addServiceDependency(si1_2_1);
        testDao.saveServiceHierarchy(si1);

        List<JacsServiceData> s1Hierarchy = testDao.findServiceHierarchy(si1.getId()).serviceHierarchyStream().collect(Collectors.toList());;
        assertThat(s1Hierarchy.size(), equalTo(5));
        assertThat(s1Hierarchy.subList(1, s1Hierarchy.size()), everyItem(Matchers.hasProperty("rootServiceId", equalTo(si1.getId()))));
        assertThat(s1Hierarchy.get(0), Matchers.hasProperty("rootServiceId", Matchers.nullValue()));

        List<JacsServiceData> s1_1_Hierarchy = testDao.findServiceHierarchy(si1_1.getId()).serviceHierarchyStream().collect(Collectors.toList());;
        assertThat(s1_1_Hierarchy.size(), equalTo(4));
        assertThat(s1_1_Hierarchy, everyItem(Matchers.hasProperty("rootServiceId", equalTo(si1.getId()))));

        List<JacsServiceData> s1_2_Hierarchy = testDao.findServiceHierarchy(si1_2.getId()).serviceHierarchyStream().collect(Collectors.toList());;
        assertThat(s1_2_Hierarchy.size(), equalTo(2));
        assertThat(s1_2_Hierarchy, everyItem(Matchers.hasProperty("rootServiceId", equalTo(si1.getId()))));

        List<JacsServiceData> s1_2_1_Hierarchy = testDao.findServiceHierarchy(si1_2_1.getId()).serviceHierarchyStream().collect(Collectors.toList());;
        assertThat(s1_2_1_Hierarchy.size(), equalTo(1));
        assertThat(s1_2_1_Hierarchy, everyItem(Matchers.hasProperty("rootServiceId", equalTo(si1.getId()))));
    }

    @Test
    public void retrieveServicesByState() {
        List<JacsServiceData> servicesInQueuedState = ImmutableList.of(
                createTestService("s1.1", ProcessingLocation.LOCAL),
                createTestService("s1.2", ProcessingLocation.LOCAL),
                createTestService("s1.3", ProcessingLocation.LOCAL),
                createTestService("s1.4", ProcessingLocation.LOCAL)
        );
        List<JacsServiceData> servicesInRunningState = ImmutableList.of(
                createTestService("s2.4", ProcessingLocation.SGE_DRMAA),
                createTestService("s2.5", ProcessingLocation.SGE_DRMAA),
                createTestService("s2.6", ProcessingLocation.SGE_DRMAA)
        );
        List<JacsServiceData> servicesInCanceledState = ImmutableList.of(
                createTestService("s7", null),
                createTestService("s8", null),
                createTestService("s9", null)
        );
        servicesInQueuedState.stream().forEach(s -> {
            s.setState(JacsServiceState.QUEUED);
            persistServiceWithEvents(s, ImmutableMap.of(
                            "s1", new RegisteredJacsNotification().addNotificationField("nf1", "nv1").withDefaultLifecycleStages(),
                            "s2", new RegisteredJacsNotification().addNotificationField("nf2", "nv2").withDefaultLifecycleStages()
                    )
            );
        });
        servicesInRunningState.stream().forEach(s -> {
            s.setState(JacsServiceState.RUNNING);
            persistServiceWithEvents(s, ImmutableMap.of());
        });
        servicesInCanceledState.stream().forEach(s -> {
            s.setState(JacsServiceState.CANCELED);
            persistServiceWithEvents(s, ImmutableMap.of(
                            "s1", new RegisteredJacsNotification().addNotificationField("nf1", "nv1").forLifecycleStage(JacsServiceLifecycleStage.START_PROCESSING)
                    )
            );
        });
        PageRequest pageRequest = new PageRequest();
        PageResult<JacsServiceData> retrievedQueuedServices = testDao.findServicesByState(ImmutableSet.of(JacsServiceState.QUEUED), pageRequest);
        assertThat(retrievedQueuedServices.getResultList(), everyItem(Matchers.hasProperty("state", equalTo(JacsServiceState.QUEUED))));
        assertThat(retrievedQueuedServices.getResultList().size(), equalTo(servicesInQueuedState.size()));

        PageResult<JacsServiceData> retrievedRunningOrCanceledServices = testDao.findServicesByState(
                ImmutableSet.of(JacsServiceState.RUNNING, JacsServiceState.CANCELED), pageRequest);
        assertThat(retrievedRunningOrCanceledServices.getResultList().size(), equalTo(servicesInRunningState.size() + servicesInCanceledState.size()));
    }

    @Test
    public void claimUnassignedFirstThenAssignedServices() {
        List<JacsServiceData> servicesInQueuedState = ImmutableList.of(
                createTestService("s1.1", ProcessingLocation.LOCAL),
                createTestService("s1.2", ProcessingLocation.LOCAL),
                createTestService("s1.3", ProcessingLocation.LOCAL),
                createTestService("s1.4", ProcessingLocation.LOCAL)
        );
        List<JacsServiceData> servicesInRunningState = ImmutableList.of(
                createTestService("s2.4", ProcessingLocation.SGE_DRMAA),
                createTestService("s2.5", ProcessingLocation.SGE_DRMAA),
                createTestService("s2.6", ProcessingLocation.SGE_DRMAA)
        );
        List<JacsServiceData> servicesInCanceledState = ImmutableList.of(
                createTestService("s7", null),
                createTestService("s8", null),
                createTestService("s9", null)
        );
        servicesInQueuedState.stream().forEach(s -> {
            s.setState(JacsServiceState.QUEUED);
            persistServiceWithEvents(s, ImmutableMap.of());
        });
        servicesInRunningState.stream().forEach(s -> {
            s.setState(JacsServiceState.RUNNING);
            persistServiceWithEvents(s, ImmutableMap.of());
        });
        servicesInCanceledState.stream().forEach(s -> {
            s.setState(JacsServiceState.CANCELED);
            persistServiceWithEvents(s, ImmutableMap.of());
        });
        String testQueueId = "testQueueId";
        PageRequest pageRequest = new PageRequest();
        PageResult<JacsServiceData> retrievedQueuedServices;

        // first claim unassigned services
        retrievedQueuedServices = testDao.claimServiceByQueueAndState(testQueueId, ImmutableSet.of(JacsServiceState.QUEUED), pageRequest);
        assertThat(retrievedQueuedServices.getResultList(), everyItem(Matchers.hasProperty("state", equalTo(JacsServiceState.QUEUED))));
        assertThat(retrievedQueuedServices.getResultList(), everyItem(Matchers.hasProperty("queueId", equalTo(testQueueId))));
        assertThat(retrievedQueuedServices.getResultList().size(), equalTo(servicesInQueuedState.size()));
        // now try to claim them for a different queue
        retrievedQueuedServices = testDao.claimServiceByQueueAndState("otherQueue", ImmutableSet.of(JacsServiceState.QUEUED), pageRequest);
        assertThat(retrievedQueuedServices.getResultList(), emptyCollectionOf(JacsServiceData.class));
        // then claim them again for the same queue that claimed them first
        retrievedQueuedServices = testDao.claimServiceByQueueAndState(testQueueId, ImmutableSet.of(JacsServiceState.QUEUED), pageRequest);
        assertThat(retrievedQueuedServices.getResultList(), everyItem(Matchers.hasProperty("state", equalTo(JacsServiceState.QUEUED))));
        assertThat(retrievedQueuedServices.getResultList(), everyItem(Matchers.hasProperty("queueId", equalTo(testQueueId))));
        assertThat(retrievedQueuedServices.getResultList().size(), equalTo(servicesInQueuedState.size()));

        PageResult<JacsServiceData> retrievedRunningOrCanceledServices = testDao.findServicesByState(
                ImmutableSet.of(JacsServiceState.RUNNING, JacsServiceState.CANCELED), pageRequest);
        assertThat(retrievedRunningOrCanceledServices.getResultList().size(), equalTo(servicesInRunningState.size() + servicesInCanceledState.size()));
    }

    @Test
    public void simulateConcurrentClaim() {
        List<JacsServiceData> servicesInQueuedState = ImmutableList.of(
                createTestService("s1.1", ProcessingLocation.LOCAL),
                createTestService("s1.2", ProcessingLocation.LOCAL),
                createTestService("s1.3", ProcessingLocation.LOCAL),
                createTestService("s1.4", ProcessingLocation.LOCAL)
        );
        List<JacsServiceData> servicesInRunningState = ImmutableList.of(
                createTestService("s2.4", ProcessingLocation.SGE_DRMAA),
                createTestService("s2.5", ProcessingLocation.SGE_DRMAA),
                createTestService("s2.6", ProcessingLocation.SGE_DRMAA)
        );
        List<JacsServiceData> servicesInCanceledState = ImmutableList.of(
                createTestService("s7", null),
                createTestService("s8", null),
                createTestService("s9", null)
        );
        servicesInQueuedState.stream().forEach(s -> {
            s.setState(JacsServiceState.QUEUED);
            persistServiceWithEvents(s, ImmutableMap.of());
        });
        servicesInRunningState.stream().forEach(s -> {
            s.setState(JacsServiceState.RUNNING);
            persistServiceWithEvents(s, ImmutableMap.of());
        });
        servicesInCanceledState.stream().forEach(s -> {
            s.setState(JacsServiceState.CANCELED);
            persistServiceWithEvents(s, ImmutableMap.of());
        });
        String testQueueId = "testQueueId";
        PageRequest pageRequest = new PageRequest();

        JacsServiceDataMongoDao spiedTestDao = new JacsServiceDataMongoDao(testMongoDatabase, idGenerator) {
            @Override
            protected Class<JacsServiceData> getEntityType() {
                return JacsServiceData.class;
            }

            @Override
            protected <R> List<R> find(Bson queryFilter, Bson sortCriteria, long offset, int length, Class<R> resultType) {
                List<R> candidates = super.find(queryFilter, sortCriteria, offset, length, resultType);
                // while one queue is trying to claim the services some other queue attempts to do the same
                // and the other queue manages to get the services first
                candidates.stream().forEach(e -> {
                    JacsServiceData sd = (JacsServiceData) e;
                    mongoCollection.findOneAndUpdate(
                            Filters.and(Filters.eq("_id", sd.getId())),
                            Updates.combine(
                                    Updates.set("queueId", "otherqueue")
                            )
                    );
                });
                return candidates;
            }
        };
        PageResult<JacsServiceData> retrievedQueuedServices = spiedTestDao.claimServiceByQueueAndState(testQueueId, ImmutableSet.of(JacsServiceState.QUEUED), pageRequest);
        assertThat(retrievedQueuedServices.getResultList(), emptyCollectionOf(JacsServiceData.class));
    }

    @Test
    public void searchServicesByUserStateAndDateRange() {
        Calendar testCal = Calendar.getInstance();
        testCal.add(Calendar.DATE, -100);
        Date startDate = testCal.getTime();
        testCal.add(Calendar.DATE, 1);
        List<JacsServiceData> testServices = persistServicesForSearchTest(testCal);
        testCal.add(Calendar.DATE, 1);
        Date endDate = testCal.getTime();

        JacsServiceData emptyRequest = new JacsServiceData();
        PageRequest pageRequest = new PageRequest();
        PageResult<JacsServiceData> retrievedQueuedServices;

        retrievedQueuedServices = testDao.findMatchingServices(emptyRequest, new DataInterval<>(null, null), pageRequest);
        assertThat(retrievedQueuedServices.getResultList(), everyItem(Matchers.hasProperty("id", Matchers.isIn(testServices.stream().map(e->e.getId()).toArray()))));

        JacsServiceData u1ServicesRequest = new JacsServiceData();
        u1ServicesRequest.setOwner("user:u1");
        u1ServicesRequest.setState(JacsServiceState.QUEUED);

        retrievedQueuedServices = testDao.findMatchingServices(u1ServicesRequest, new DataInterval<>(null, null), pageRequest);
        assertThat(retrievedQueuedServices.getResultList(), everyItem(Matchers.hasProperty("state", equalTo(JacsServiceState.QUEUED))));
        assertThat(retrievedQueuedServices.getResultList(), everyItem(Matchers.hasProperty("owner", equalTo("user:u1"))));

        retrievedQueuedServices = testDao.findMatchingServices(u1ServicesRequest, new DataInterval<>(startDate, endDate), pageRequest);
        assertThat(retrievedQueuedServices.getResultList(), everyItem(Matchers.hasProperty("state", equalTo(JacsServiceState.QUEUED))));
        assertThat(retrievedQueuedServices.getResultList(), everyItem(Matchers.hasProperty("owner", equalTo("user:u1"))));

        retrievedQueuedServices = testDao.findMatchingServices(u1ServicesRequest, new DataInterval<>(null, startDate), pageRequest);
        assertThat(retrievedQueuedServices.getResultList(), hasSize(0));

        retrievedQueuedServices = testDao.findMatchingServices(u1ServicesRequest, new DataInterval<>(endDate, null), pageRequest);
        assertThat(retrievedQueuedServices.getResultList(), hasSize(0));
    }

    @Test
    public void archiveServiceHierarchy() {
        JacsServiceData si1 = createTestService("s1", ProcessingLocation.LOCAL);
        JacsServiceData si1_1 = createTestService("s1.1", ProcessingLocation.LOCAL);
        JacsServiceData si1_2 = createTestService("s1.2", ProcessingLocation.LOCAL);
        JacsServiceData si1_3 = createTestService("s1.3", ProcessingLocation.LOCAL);
        JacsServiceData si1_2_1 = createTestService("s1.2.1", ProcessingLocation.LOCAL);
        si1.addServiceDependency(si1_1);
        si1.addServiceDependency(si1_2);
        si1_1.addServiceDependency(si1_2);
        si1_1.addServiceDependency(si1_3);
        si1_2.addServiceDependency(si1_2_1);
        testDao.saveServiceHierarchy(si1);
        List<JacsServiceData> s1Hierarchy = si1.serviceHierarchyStream().collect(Collectors.toList());
        testDao.archiveServiceHierarchy(si1);

        s1Hierarchy.forEach(sd -> {
            JacsServiceData retrievedServiceData = testDao.findById(sd.getId());
            assertNull(retrievedServiceData);
        });
    }

    private List<JacsServiceData> persistServicesForSearchTest(Calendar calDate) {
        List<JacsServiceData> testServices = new ArrayList<>();
        List<JacsServiceData> u1Services = ImmutableList.of(
                createTestService("s1.1", ProcessingLocation.LOCAL),
                createTestService("s1.2", ProcessingLocation.LOCAL),
                createTestService("s1.3", ProcessingLocation.LOCAL),
                createTestService("s1.4", ProcessingLocation.LOCAL)
        );
        List<JacsServiceData> u2Services = ImmutableList.of(
                createTestService("s2.1", ProcessingLocation.SGE_DRMAA),
                createTestService("s2.2", ProcessingLocation.SGE_DRMAA),
                createTestService("s2.3", ProcessingLocation.SGE_DRMAA)
        );
        u1Services.stream().forEach(s -> {
            s.setOwner("user:u1");
            s.setState(JacsServiceState.QUEUED);
            s.setCreationDate(calDate.getTime());
            calDate.add(Calendar.DATE, 1);
            persistServiceWithEvents(s, ImmutableMap.of());
            testServices.add(s);
        });
        u2Services.stream().forEach(s -> {
            s.setOwner("group:u2");
            s.setState(JacsServiceState.RUNNING);
            s.setCreationDate(calDate.getTime());
            calDate.add(Calendar.DATE, 1);
            persistServiceWithEvents(s, ImmutableMap.of());
            testServices.add(s);
        });
        return testServices;
    }

    private JacsServiceData persistServiceWithEvents(JacsServiceData si, Map<String, RegisteredJacsNotification> processingStageNotifications, JacsServiceEvent... jacsServiceEvents) {
        for (JacsServiceEvent se : jacsServiceEvents) {
            si.addNewEvent(se);
        }
        processingStageNotifications.forEach(si::setProcessingStageNotification);
        testDao.save(si);
        return si;
    }

    protected List<JacsServiceData> createMultipleTestItems(int nItems) {
        List<JacsServiceData> testItems = new ArrayList<>();
        for (int i = 0; i < nItems; i++) {
            testItems.add(createTestService("s" + (i + 1), i % 2 == 0 ? ProcessingLocation.LOCAL : ProcessingLocation.SGE_DRMAA));
        }
        return testItems;
    }

    private JacsServiceData createTestService(String serviceName, ProcessingLocation processingLocation) {
        JacsServiceData si = new JacsServiceData();
        si.setName(serviceName);
        si.setProcessingLocation(processingLocation);
        si.addArg("I1");
        si.addArg("I2");
        testData.add(si);
        return si;
    }

    private JacsServiceEvent createTestServiceEvent(String name, String value) {
        JacsServiceEvent se = new JacsServiceEvent();
        se.setName(name);
        se.setValue(value);
        return se;
    }

}
