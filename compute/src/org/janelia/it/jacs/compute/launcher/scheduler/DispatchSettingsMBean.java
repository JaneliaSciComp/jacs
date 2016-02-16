package org.janelia.it.jacs.compute.launcher.scheduler;

import org.jboss.annotation.ejb.Management;

import javax.management.MXBean;

public interface DispatchSettingsMBean {
    String DISPATCHER_SETTINGS_JNDI_NAME = "/service/dispatcherSettings";

    String getCurrentProcessingId();
    void setCurrentProcessingId(String currentProcessingId);
    int getMaxRetries();
    void setMaxRetries(int maxRetries);
    int getPrefetchSize();
    void setPrefetchSize(int prefetchSize);
    void start();
    void stop();
}
