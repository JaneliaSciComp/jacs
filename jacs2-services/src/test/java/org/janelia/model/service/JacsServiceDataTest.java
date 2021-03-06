package org.janelia.model.service;

import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;

public class JacsServiceDataTest {

    @SuppressWarnings("unchecked")
    @Test
    public void dependenciesTraversal() {
        JacsServiceData root = createJacsServiceData("n0", 1);
        JacsServiceData n1_1 = createJacsServiceData("n1.1", 1);
        JacsServiceData n1_2 = createJacsServiceData("n1.2", 1);
        JacsServiceData n1_1_1 = createJacsServiceData("n1.1.1", 1);
        JacsServiceData n1_1_2 = createJacsServiceData("n1.1.2", 1);
        JacsServiceData n1_2_1 = createJacsServiceData("n1.2.1", 1);

        root.addServiceDependency(n1_1);
        root.addServiceDependency(n1_2);
        n1_1.addServiceDependency(n1_2);
        n1_1.addServiceDependency(n1_1_1);
        n1_1.addServiceDependency(n1_1_2);
        n1_1_2.addServiceDependency(n1_1_1);
        n1_2.addServiceDependency(n1_2_1);

        List<JacsServiceData> rootHierarchy = root.serviceHierarchyStream().collect(Collectors.toList());
        assertThat(rootHierarchy, contains(root, n1_1, n1_2, n1_2_1, n1_1_1, n1_1_2));
        assertThat(rootHierarchy, contains(
                        allOf(hasProperty("name", equalTo("n0")),
                                hasProperty("priority", equalTo(1))),
                        allOf(hasProperty("name", equalTo("n1.1")),
                                hasProperty("priority", equalTo(2))),
                        allOf(hasProperty("name", equalTo("n1.2")),
                                hasProperty("priority", equalTo(3))), // n1_1 depends on n1_2 so n1_2 has a higher priority
                        allOf(hasProperty("name", equalTo("n1.2.1")),
                                hasProperty("priority", equalTo(4))),
                        allOf(hasProperty("name", equalTo("n1.1.1")),
                                hasProperty("priority", equalTo(4))),
                        allOf(hasProperty("name", equalTo("n1.1.2")),
                                hasProperty("priority", equalTo(3)))
                )
        );

        List<JacsServiceData> n1_1Hierarchy = n1_1.serviceHierarchyStream().collect(Collectors.toList());
        assertThat(n1_1Hierarchy, contains(n1_1, n1_2, n1_2_1, n1_1_1, n1_1_2));
    }

    private JacsServiceData createJacsServiceData(String name, int priority) {
        JacsServiceData sd = new JacsServiceData();
        sd.setName(name);
        sd.setPriority(priority);
        return sd;
    }
}
