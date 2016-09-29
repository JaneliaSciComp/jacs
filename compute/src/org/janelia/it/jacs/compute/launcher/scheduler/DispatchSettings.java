package org.janelia.it.jacs.compute.launcher.scheduler;

import org.janelia.it.jacs.compute.mbean.AbstractComponentMBean;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;


@Singleton
@Startup
public class DispatchSettings extends AbstractComponentMBean implements DispatchSettingsMBean {
    private static final Logger LOG = LoggerFactory.getLogger(DispatchSettings.class);

    private String currentProcessingId = SystemConfigurationProperties.getString("System.ServerName");
    private boolean fetchUnassignedJobs = SystemConfigurationProperties.getBoolean("computeserver.dispatch.fetchUnassignedJobs", false);
    private int maxRetries = SystemConfigurationProperties.getInt("computeserver.dispatch.maxRetries");
    private int prefetchSize = SystemConfigurationProperties.getInt("computeserver.dispatch.prefetchSize");

    public DispatchSettings() {
        super("jacs");
    }

    @Override
    public String getCurrentProcessingId() {
        return currentProcessingId;
    }

    @Override
    public void setCurrentProcessingId(String currentProcessingId) {
        this.currentProcessingId = currentProcessingId;
    }

    @Override
    public boolean isFetchUnassignedJobs() {
        return fetchUnassignedJobs;
    }

    @Override
    public void setFetchUnassignedJobs(boolean fetchUnassignedJobs) {
        this.fetchUnassignedJobs = fetchUnassignedJobs;
    }

    @Override
    public int getMaxRetries() {
        return maxRetries;
    }

    @Override
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    @Override
    public int getPrefetchSize() {
        return prefetchSize;
    }

    @Override
    public void setPrefetchSize(int prefetchSize) {
        this.prefetchSize = prefetchSize;
    }

//    @Override
//    public void start() {
//        try {
//            InitialContext rootCtx = new InitialContext();
//            Name fullName = rootCtx.getNameParser("").parse(DISPATCHER_SETTINGS_JNDI_NAME);
//            NonSerializableFactory.rebind(fullName, this, true);
//        } catch (NamingException e) {
//            LOG.error("Failed to bind dispatcher settings", e);
//        }
//    }
//
//    @Override
//    public void stop() {
//        try {
//            InitialContext rootCtx = new InitialContext();
//            rootCtx.unbind(DISPATCHER_SETTINGS_JNDI_NAME);
//            NonSerializableFactory.unbind(DISPATCHER_SETTINGS_JNDI_NAME);
//        } catch(NamingException e) {
//            LOG.error("Failed to unbind dispatcher settings", e);
//        }
//    }
}
