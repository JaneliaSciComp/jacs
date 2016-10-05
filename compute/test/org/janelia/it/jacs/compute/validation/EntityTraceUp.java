package org.janelia.it.jacs.compute.validation;

import org.janelia.it.jacs.compute.api.EntityBeanRemote;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.junit.Test;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

/**
 * Trace upward from some entity, all the way to its "root"--that point at which parent == null.  Tell much
 * information about each step along the trace.
 *
 * @author fosterl
 */
public class EntityTraceUp {
    private static final Long TEST_GUID = 1851591489742700642L;
    private static final String DEFAULT_SERVER = "remote://jacs-staging:1199";
    private boolean dumpValues = false;
    private String server = DEFAULT_SERVER;

    @Test
    public void traceUpTest() {
        traceUp(TEST_GUID);
    }

    public void traceUp(Long guid) {
        try {
            EntityBeanRemote eobj = getEntityBeanRemote();
            traverse( guid, eobj, "LEAF" );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String [] args) {
        EntityTraceUp ct = new EntityTraceUp();
        Long guid;
        try {
            guid = Long.parseLong( args[ 0 ] );
        } catch ( Exception ex ) {
            throw new IllegalArgumentException( "USAGE: java " + EntityTraceUp.class.getName() + " <GUID>" );
        }
        if ( args.length >= 2 ) {
            ct.setDumpValues( Boolean.parseBoolean(args[1]) );
        }
        if ( args.length >= 3 ) {
            ct.setServer( args[ 2 ] );
        }
        ct.traceUp(guid);

    }

    @SuppressWarnings("unused")
    public boolean isDumpValues() {
        return dumpValues;
    }

    public void setDumpValues(boolean dumpValues) {
        this.dumpValues = dumpValues;
    }

    public void setServer( String server ) {
        this.server = server;
    }

    private EntityBeanRemote getEntityBeanRemote() throws NamingException {
        Hashtable<String,String> environment = new Hashtable<>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
        environment.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        environment.put(Context.PROVIDER_URL, server);
        environment.put(Context.SECURITY_PRINCIPAL, "jmsuser");
        environment.put(Context.SECURITY_CREDENTIALS, "jmsuser");
        InitialContext context = new InitialContext(environment);
        return (EntityBeanRemote) context.lookup("compute/EntityEJB/remote");
    }

    /**
     * Recursively walk up the parentage / ancestry tree to find the root.
     * @param guid target entity id.
     * @param eobj for obtaining database info.
     * @param breadCrumb Trace header with full stack of ancestors to this point.
     * @throws Exception
     */
    private void traverse( Long guid, EntityBeanRemote eobj, String breadCrumb ) throws Exception {
        Entity entity = eobj.getEntityAndChildren( null, guid );
        System.out.println("===============================================  " + entity.getName() + " / " + guid );
        breadCrumb = entity.getEntityTypeName() + " (" + entity.getName() + ":" + entity.getId() + ")" + " --> " + breadCrumb;
        System.out.println(breadCrumb);
        System.out.println("Entity Type=" + entity.getEntityTypeName());
        List<EntityData> entityDatas = entity.getOrderedEntityData();

        if ( dumpValues ) {
            for ( EntityData entityData: entityDatas ) {
                Entity childEntity = entityData.getChildEntity();
                String prefix = entityData.getValue();
                if ( prefix == null ) {
                    prefix = "";
                }
                else {
                    prefix += "/";
                }
                String appended = "";
                if ( childEntity != null ) {
                    appended += "/" + childEntity.getEntityTypeName() + "/" + childEntity.getName();
                }

                System.out.println( entityData.getEntityAttrName() + "=" + prefix + appended );
            }
            System.out.println();
        }

        Set<Entity> parentEntities = eobj.getParentEntities( null, guid );
        if ( parentEntities != null  &&  ! parentEntities.isEmpty() ) {
            for ( Entity parent: parentEntities ) {
                traverse( parent.getId(), eobj, breadCrumb );
            }
        }
        else {
            System.out.println( "[ROOT]" );
        }

    }

}
