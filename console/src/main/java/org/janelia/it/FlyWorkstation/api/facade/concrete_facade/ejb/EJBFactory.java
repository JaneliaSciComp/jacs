package org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.janelia.it.FlyWorkstation.gui.util.ConsoleProperties;
import org.janelia.it.jacs.compute.api.*;

public class EJBFactory {
    private static final String PROVIDER_URL = ConsoleProperties.getInstance().getProperty("provider.url");
    private static final String INITIAL_CONTEXT_FACTORY = ConsoleProperties.getInstance().getProperty("initial.context.factory");
    private static final String URL_PKG_PREFIXES = ConsoleProperties.getInstance().getProperty("url.pkg.prefixes");
    private static final String REMOTE_ANNOTATION_JNDI_NAME = ConsoleProperties.getInstance().getProperty("remote.annotation.jndi.name");
    private static final String REMOTE_SOLR_JNDI_NAME = ConsoleProperties.getInstance().getProperty("remote.solr.jndi.name");
    private static final String REMOTE_COMPUTE_JNDI_NAME = ConsoleProperties.getInstance().getProperty("remote.compute.jndi.name");
    private static final String REMOTE_ENTITY_JNDI_NAME = ConsoleProperties.getInstance().getProperty("remote.entity.jndi.name");
    private static final String REMOTE_SEARCH_JNDI_NAME = ConsoleProperties.getInstance().getProperty("remote.search.jndi.name");
    private static final String REMOTE_GENOME_CONTEXT_JNDI_NAME = ConsoleProperties.getInstance().getProperty("remote.genome.context.jndi.name");
    private static final String REMOTE_JOB_CONTROL_JNDI_NAME = ConsoleProperties.getInstance().getProperty("remote.job.control.jndi.name");
    private static Properties icProperties = new Properties();

    static {
        icProperties.put(Context.PROVIDER_URL, PROVIDER_URL);
        icProperties.put(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
        icProperties.put(Context.URL_PKG_PREFIXES, URL_PKG_PREFIXES);
    }

    /**
     * Create the InitialContext every time to avoid concurrency issues
     *
     * @return InitialContext
     * @throws javax.naming.NamingException problem with the name
     */
    private static InitialContext createInitialContext() throws NamingException {
        return new InitialContext(icProperties);
    }

    /**
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
            e.printStackTrace();
            return null;
        }
    }

    /**
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
    
    public static SearchBeanRemote getRemoteSearchBean() {
        return (SearchBeanRemote) getRemoteInterface(REMOTE_SEARCH_JNDI_NAME);
    }

    public static SolrBeanRemote getRemoteSolrBean() {
        return (SolrBeanRemote) getRemoteInterface(REMOTE_SOLR_JNDI_NAME);
    }

    public static GenomeContextBeanRemote getRemoteGenomeContextBean() {
        return (GenomeContextBeanRemote) getRemoteInterface(REMOTE_GENOME_CONTEXT_JNDI_NAME);
    }

    public static JobControlBeanRemote getRemoteJobControlBean() {
        return (JobControlBeanRemote) getRemoteInterface(REMOTE_JOB_CONTROL_JNDI_NAME);
    }


}