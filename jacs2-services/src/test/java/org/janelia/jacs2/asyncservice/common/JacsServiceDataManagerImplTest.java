package org.janelia.jacs2.asyncservice.common;

import com.google.common.collect.ImmutableMap;
import org.janelia.jacs2.asyncservice.JacsServiceDataManager;
import org.janelia.jacs2.asyncservice.impl.JacsServiceDataManagerImpl;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.model.jacs2.AppendFieldValueHandler;
import org.janelia.model.jacs2.SetFieldValueHandler;
import org.janelia.model.service.JacsServiceData;
import org.janelia.model.service.JacsServiceEventTypes;
import org.janelia.model.service.JacsServiceState;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JacsServiceDataManagerImplTest {

    private static final Long TEST_ID = 1L;

    private JacsServiceDataPersistence jacsServiceDataPersistence;
    private JacsServiceDataManager jacsServiceDataManager;

    @Before
    public void setUp() {
        jacsServiceDataPersistence = mock(JacsServiceDataPersistence.class);
        jacsServiceDataManager = new JacsServiceDataManagerImpl(jacsServiceDataPersistence);
    }

    @Test
    public void updateMissingService() {
        when(jacsServiceDataPersistence.findServiceHierarchy(TEST_ID)).thenReturn(null);
        assertNull(jacsServiceDataManager.updateService(TEST_ID, createServiceData("test", JacsServiceState.QUEUED, 1)));
    }

    @Test
    public void updateExistingServiceStateAndPriority() {
        JacsServiceData existingData = createServiceData("test", JacsServiceState.CREATED, 1);
        JacsServiceData newData = createServiceData("test", JacsServiceState.RUNNING, 1);
        when(jacsServiceDataPersistence.findServiceHierarchy(TEST_ID)).thenReturn(existingData);
        JacsServiceData updatedData = jacsServiceDataManager.updateService(TEST_ID, newData);
        assertThat(updatedData.getState(), equalTo(newData.getState()));
        verify(jacsServiceDataPersistence).update(existingData,
                ImmutableMap.of(
                        "events", new AppendFieldValueHandler<>(JacsServiceData.createServiceEvent(JacsServiceEventTypes.UPDATE_STATE, "Update state from CREATED -> RUNNING")),
                        "state", new SetFieldValueHandler<>(JacsServiceState.RUNNING)
                ));
    }

    @Test
    public void updateExistingServiceStateOnly() {
        JacsServiceData existingData = createServiceData("test", JacsServiceState.CREATED, 1);
        JacsServiceData newData = createServiceData("test", JacsServiceState.RUNNING, null);
        when(jacsServiceDataPersistence.findServiceHierarchy(TEST_ID)).thenReturn(existingData);
        JacsServiceData updatedData = jacsServiceDataManager.updateService(TEST_ID, newData);
        assertThat(updatedData.getState(), equalTo(newData.getState()));
        verify(jacsServiceDataPersistence).update(existingData,
                ImmutableMap.of(
                        "events", new AppendFieldValueHandler<>(JacsServiceData.createServiceEvent(JacsServiceEventTypes.UPDATE_STATE, "Update state from CREATED -> RUNNING")),
                        "state", new SetFieldValueHandler<>(JacsServiceState.RUNNING)
                ));
    }

    private JacsServiceData createServiceData(String name, JacsServiceState state, Integer priority) {
        JacsServiceData sd = new JacsServiceData();
        sd.setId(TEST_ID);
        sd.setName(name);
        sd.setState(state);
        sd.setPriority(priority);
        return sd;
    }
}