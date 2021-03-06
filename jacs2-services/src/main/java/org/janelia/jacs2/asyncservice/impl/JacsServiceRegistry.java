package org.janelia.jacs2.asyncservice.impl;

import org.janelia.jacs2.asyncservice.common.ServiceProcessor;
import org.janelia.model.service.ServiceMetaData;
import org.janelia.jacs2.asyncservice.ServiceRegistry;
import org.slf4j.Logger;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JacsServiceRegistry implements ServiceRegistry {

    private final Instance<ServiceProcessor<?>> anyServiceSource;
    private final Logger logger;

    @Inject
    public JacsServiceRegistry(@Any Instance<ServiceProcessor<?>> anyServiceSource, Logger logger) {
        this.anyServiceSource = anyServiceSource;
        this.logger = logger;
    }

    @Override
    public ServiceMetaData getServiceMetadata(String serviceName) {
        ServiceProcessor service = lookupService(serviceName);
        return service != null ? service.getMetadata() : null;
    }

    @Override
    public List<ServiceMetaData> getAllServicesMetadata() {
        return getAllServices(anyServiceSource).stream().map(ServiceProcessor::getMetadata).collect(Collectors.toList());
    }

    @Override
    public ServiceProcessor<?> lookupService(String serviceName) {
        try {
            for (ServiceProcessor<?> service : getAllServices(anyServiceSource)) {
                if (serviceName.equals(service.getMetadata().getServiceName())) {
                    logger.trace("Service found for {}", serviceName);
                    return service;
                }
            }
            logger.error("NO Service found for {}", serviceName);
        } catch (Throwable e) {
            logger.error("Error while looking up {}", serviceName, e);
        }
        return null;
    }

    private List<ServiceProcessor<?>> getAllServices(@Any Instance<ServiceProcessor<?>> services) {
        List<ServiceProcessor<?>> allServices = new ArrayList<>();
        for (ServiceProcessor<?> service : services) {
            allServices.add(service);
        }
        return allServices;
    }

}
