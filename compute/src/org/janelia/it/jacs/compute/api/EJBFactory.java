
package org.janelia.it.jacs.compute.api;

import org.janelia.it.jacs.compute.engine.launcher.ILauncher;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;

import javax.ejb.EJBException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;

/**
 * This class returns interfaces for local EJBs.  It is also used by JUnit tests to return the remote
 * interface
 *
 * @author Tareq Nabeel
 */
public class EJBFactory {
    private static final String PROVIDER_URL = "jnp://" + SystemConfigurationProperties.getString("computeserver.ejb.service");
    private static final String INITIAL_CONTEXT_FACTORY = "org.jnp.interfaces.NamingContextFactory";
    private static final String URL_PKG_PREFIXES = "org.jboss.naming:org.jnp.interfaces";
    private static Properties icProperties = new Properties();

    public static final String LOCAL_ANNOTATION_JNDI_NAME = "compute/" + SystemConfigurationProperties.getString("AnnotationEJB.Name") + "/local";
    public static final String REMOTE_ANNOTATION_JNDI_NAME = "compute/" + SystemConfigurationProperties.getString("AnnotationEJB.Name") + "/remote";
    public static final String LOCAL_COMPUTE_JNDI_NAME = "compute/" + SystemConfigurationProperties.getString("ComputeEJB.Name") + "/local";
    public static final String REMOTE_COMPUTE_JNDI_NAME = "compute/" + SystemConfigurationProperties.getString("ComputeEJB.Name") + "/remote";
    public static final String LOCAL_ENTITY_JNDI_NAME = "compute/" + SystemConfigurationProperties.getString("EntityEJB.Name") + "/local";
    public static final String REMOTE_ENTITY_JNDI_NAME = "compute/" + SystemConfigurationProperties.getString("EntityEJB.Name") + "/remote";
    public static final String LOCAL_SEARCH_JNDI_NAME = "compute/" + SystemConfigurationProperties.getString("SearchEJB.Name") + "/local";
    public static final String REMOTE_SEARCH_JNDI_NAME = "compute/" + SystemConfigurationProperties.getString("SearchEJB.Name") + "/remote";
    public static final String LOCAL_SOLR_JNDI_NAME = "compute/" + SystemConfigurationProperties.getString("SolrEJB.Name") + "/local";
    public static final String REMOTE_SOLR_JNDI_NAME = "compute/" + SystemConfigurationProperties.getString("SolrEJB.Name") + "/remote";
    public static final String LOCAL_JOB_CONTROL_JNDI_NAME = "compute/" + SystemConfigurationProperties.getString("JobControlEJB.Name") + "/local";
    public static final String REMOTE_JOB_CONTROL_JNDI_NAME = "compute/" + SystemConfigurationProperties.getString("JobControlEJB.Name") + "/remote";
    public static final String LOCAL_TILED_MICROSCOPE_JNDI_NAME = "compute/" + SystemConfigurationProperties.getString("TiledMicroscopeEJB.Name") + "/local";
    public static final String REMOTE_TILED_MICROSCOPE_JNDI_NAME = "compute/" + SystemConfigurationProperties.getString("TiledMicroscopeEJB.Name") + "/remote";

    static {
        icProperties.put(Context.PROVIDER_URL, PROVIDER_URL);
        icProperties.put(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
        icProperties.put(Context.URL_PKG_PREFIXES, URL_PKG_PREFIXES);
    }

    /**
     * Create the InitialContext everytime to avoid concurrency issues
     *
     * @return InitialContext
     * @throws javax.naming.NamingException problem with the name
     */
    private static InitialContext createInitialContext() throws NamingException {
        return new InitialContext(icProperties);
    }

    /**
     * Returns the local SLSB interface for a service
     *
     * @param lookupName the jndi name for the service
     * @return the local SLSB interface for a service
     */
    public static IService getLocalServiceBean(String lookupName) {
        return (IService) getLocalInterface(lookupName);
    }

    /**
     * Returns the local SLSB interface for a launcher
     *
     * @param lookupName the jndi name for the launcher
     * @return the local SLSB interface for a launcher
     */
    public static ILauncher getLocalSeriesLauncher(String lookupName) {
        return (ILauncher) getLocalInterface(lookupName);
    }

    /**
     * Returns the local SLSB interface for the ComputeBeanImpl
     *
     * @return the local SLSB interface for the ComputeBeanImpl
     */
    public static AnnotationBeanLocal getLocalAnnotationBean() {
        return (AnnotationBeanLocal) getLocalInterface(LOCAL_ANNOTATION_JNDI_NAME);
    }

    public static ComputeBeanLocal getLocalComputeBean() {
        return (ComputeBeanLocal) getLocalInterface(LOCAL_COMPUTE_JNDI_NAME);
    }

    public static EntityBeanLocal getLocalEntityBean() {
    	EntityBeanLocal ebl = (EntityBeanLocal) getLocalInterface(LOCAL_ENTITY_JNDI_NAME);
    	return ebl;
    }
    
    public static SearchBeanLocal getLocalSearchBean() {
        return (SearchBeanLocal) getLocalInterface(LOCAL_SEARCH_JNDI_NAME);
    }
    
    public static SolrBeanLocal getLocalSolrBean() {
        return (SolrBeanLocal) getLocalInterface(LOCAL_SOLR_JNDI_NAME);
    }
    
    public static JobControlBeanLocal getLocalJobControlBean() {
        return (JobControlBeanLocal) getLocalInterface(LOCAL_JOB_CONTROL_JNDI_NAME);
    }

    public static TiledMicroscopeBeanLocal getLocalTiledMicroscopeBean() {
        return (TiledMicroscopeBeanLocal) getLocalInterface(LOCAL_TILED_MICROSCOPE_JNDI_NAME);
    }

    /**
     * Returns the EJBLocalObject for the given lookup name
     *
     * @param lookupName the jndi name for the launcher
     * @return EJBLocalObject
     */
    public static Object getLocalInterface(String lookupName) {
        try {
            InitialContext ic = createInitialContext();
            if (!lookupName.startsWith("compute/")) {
                lookupName = "compute/" + lookupName;
            }
            if (!lookupName.endsWith("/local")) {
                lookupName += "/local";
            }

            return ic.lookup(lookupName);
        }
        catch (NamingException e) {
            throw new EJBException(e);
        }
    }

    /**
     * Used by Junit test
     *
     * @param lookupName the jndi name for the ejb
     * @return EJBObject
     */
    public static Object getRemoteInterface(String lookupName) {
        try {
            InitialContext ic = createInitialContext();
            if (!lookupName.startsWith("compute/")) {
                lookupName = "compute/" + lookupName;
            }
            if (!lookupName.endsWith("/remote")) {
                lookupName += "/remote";
            }
            return ic.lookup(lookupName);
        }
        catch (NamingException e) {
            throw new EJBException(e);
        }
    }

    public static Object lookup(String lookupName) {
        try {
            InitialContext ic = createInitialContext(); 
            return ic.lookup(lookupName);
        }
        catch (NamingException e) {
            throw new EJBException(e);
        }
    }
    
    /**
     * Used by Junit test
     *
     * @return ComputeBeanRemote
     */
    public static AnnotationBeanRemote getRemoteAnnotationBean() {
        return (AnnotationBeanRemote) getRemoteInterface(REMOTE_ANNOTATION_JNDI_NAME);
    }

    public static ComputeBeanRemote getRemoteComputeBean() {
        return (ComputeBeanRemote) getRemoteInterface(REMOTE_COMPUTE_JNDI_NAME);
    }

    public static EntityBeanRemote getRemoteEntityBean() {
        return (EntityBeanRemote) getRemoteInterface(REMOTE_ENTITY_JNDI_NAME);
    }
    
//    public static SearchBeanRemote getRemoteSearchBean() {
//        return (SearchBeanRemote) getRemoteInterface(REMOTE_SEARCH_JNDI_NAME);
//    }

    public static SolrBeanRemote getRemoteSolrBean() {
        return (SolrBeanRemote) getRemoteInterface(REMOTE_SOLR_JNDI_NAME);
    }

    public static JobControlBeanRemote getRemoteJobControlBean() {
        return (JobControlBeanRemote) getRemoteInterface(REMOTE_JOB_CONTROL_JNDI_NAME);
    }

    public static TiledMicroscopeBeanRemote getRemoteTiledMicroscopeBean() {
        return (TiledMicroscopeBeanRemote) getRemoteInterface(REMOTE_TILED_MICROSCOPE_JNDI_NAME);
    }

}