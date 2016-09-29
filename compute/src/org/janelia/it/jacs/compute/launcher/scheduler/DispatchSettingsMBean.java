package org.janelia.it.jacs.compute.launcher.scheduler;

import javax.management.MXBean;

@MXBean
public interface DispatchSettingsMBean {
    String DISPATCHER_SETTINGS_JNDI_NAME = "/service/dispatcherSettings";

    String getCurrentProcessingId();
    void setCurrentProcessingId(String currentProcessingId);
    boolean isFetchUnassignedJobs();
    void setFetchUnassignedJobs(boolean fetchUnassignedJobs);
    int getMaxRetries();
    void setMaxRetries(int maxRetries);
    int getPrefetchSize();
    void setPrefetchSize(int prefetchSize);
//    void start();
//    void stop();
}
