package org.janelia.jacs2.asyncservice.common;

import com.google.common.base.Preconditions;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;
import org.janelia.jacs2.model.jacsservice.ProcessingLocation;
import org.janelia.jacs2.model.jacsservice.RegisteredJacsNotification;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ServiceExecutionContext {

    public static class Builder {
        private final ServiceExecutionContext serviceExecutionContext;

        public Builder(JacsServiceData parentServiceData) {
            Preconditions.checkArgument(parentServiceData != null);
            serviceExecutionContext = new ServiceExecutionContext(parentServiceData);
        }

        public Builder processingLocation(ProcessingLocation processingLocation) {
            serviceExecutionContext.processingLocation = processingLocation;
            return this;
        }

        public Builder waitFor(List<JacsServiceData> dependenciesList) {
            for (JacsServiceData dependency : dependenciesList) {
                if (dependency != null) serviceExecutionContext.waitFor.add(dependency);
            }
            return this;
        }

        public Builder waitFor(JacsServiceData... dependencies) {
            for (JacsServiceData dependency : dependencies) {
                if (dependency != null) serviceExecutionContext.waitFor.add(dependency);
            }
            return this;
        }

        public Builder waitFor(Number... dependenciesIds) {
            for (Number dependencyId : dependenciesIds) {
                if (dependencyId != null) serviceExecutionContext.waitForIds.add(dependencyId);
            }
            return this;
        }

        public Builder state(JacsServiceState state) {
            serviceExecutionContext.serviceState = state;
            return this;
        }

        public Builder setServiceName(String serviceName) {
            serviceExecutionContext.serviceName = serviceName;
            return this;
        }

        public Builder setOutputPath(String outputPath) {
            serviceExecutionContext.outputPath = outputPath;
            return this;
        }

        public Builder setErrorPath(String errorPath) {
            serviceExecutionContext.errorPath = errorPath;
            return this;
        }

        public Builder setWorkingDirectory(String workingDirectory) {
            serviceExecutionContext.workingDirectory = workingDirectory;
            return this;
        }

        public Builder description(String description) {
            serviceExecutionContext.description = description;
            return this;
        }

        public Builder addRequiredMemoryInGB(int mem) {
            ProcessorHelper.setRequiredMemoryInGB(serviceExecutionContext.resources, mem);
            return this;
        }

        public Builder addResources(Map<String, String> srcResources) {
            serviceExecutionContext.resources.putAll(srcResources);
            return this;
        }

        public Builder registerNotification(RegisteredJacsNotification registeredNotification) {
            if (registeredNotification != null)
                serviceExecutionContext.registeredNotifications.add(registeredNotification);
            return this;
        }

        public Builder registerNotifications(List<RegisteredJacsNotification> registeredNotifications) {
            serviceExecutionContext.registeredNotifications.addAll(registeredNotifications);
            return this;
        }

        public ServiceExecutionContext build() {
            return serviceExecutionContext;
        }
    }

    private final JacsServiceData parentServiceData;
    private ProcessingLocation processingLocation;
    private String serviceName;
    private String outputPath;
    private String errorPath;
    private String workingDirectory;
    private JacsServiceState serviceState;
    private String description;
    private final List<JacsServiceData> waitFor = new ArrayList<>();
    private final List<Number> waitForIds = new ArrayList<>();
    private final Map<String, String> resources = new LinkedHashMap<>();
    private final List<RegisteredJacsNotification> registeredNotifications = new ArrayList<>();

    private ServiceExecutionContext(JacsServiceData parentServiceData) {
        this.parentServiceData = parentServiceData;
    }

    JacsServiceData getParentServiceData() {
        return parentServiceData;
    }

    ProcessingLocation getProcessingLocation() {
        return processingLocation;
    }

    List<JacsServiceData> getWaitFor() {
        return waitFor;
    }

    List<Number> getWaitForIds() {
        return waitForIds;
    }

    JacsServiceState getServiceState() {
        return serviceState;
    }

    String getServiceName() {
        return serviceName;
    }

    String getOutputPath() {
        return outputPath;
    }

    String getErrorPath() {
        return errorPath;
    }

    String getWorkingDirectory() {
        return workingDirectory;
    }

    String getDescription() {
        return description;
    }

    Map<String, String> getResources() {
        return resources;
    }

    public List<RegisteredJacsNotification> getRegisteredNotifications() {
        return registeredNotifications;
    }
}
