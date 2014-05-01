package org.janelia.it.jacs.compute.api;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.junit.BeforeClass;

/**
 * Base class for testing the remote EJB API.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RemoteEJBTest {

    protected static InitialContext context;
    
    @BeforeClass
    public static void initContext() throws Exception {
        Hashtable environment = new Hashtable();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
        environment.put(Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces");
        environment.put(Context.PROVIDER_URL, "jnp://rokicki-ws:1199"); 
        context = new InitialContext(environment);
    }
}
