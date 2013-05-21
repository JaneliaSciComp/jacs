package org.janelia.it.jacs.compute;

import org.janelia.it.jacs.compute.api.*;
import org.janelia.it.jacs.compute.mbean.*;
import org.janelia.it.jacs.compute.api.EJBFactory;
import java.util.*;
import javax.naming.*;
import java.rmi.RemoteException;
import org.janelia.it.jacs.compute.api.support.EntityMapStep;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.compute.api.*;
import org.apache.solr.client.solrj.*;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.janelia.it.jacs.compute.access.solr.SolrDAO;
import org.janelia.it.jacs.compute.api.support.*;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.junit.*;
import static org.junit.Assert.*;

/****
	author:	Anitha Parvatham
	date  : 06/27/2012
****/

public class AnnotationTreeRoot {

    @Test
    public void testAnnotationTreeRoot() {
    try {  
    		Hashtable environment = new Hashtable();
        	environment.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
        	environment.put(Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces");
        	environment.put(Context.PROVIDER_URL, "jnp://jacs-staging:1199"); 
        	InitialContext context = new InitialContext(environment);
        	//System.out.println("-->> connected successfully to server");

                //System.out.println("\n*************************************\n"); 

        	AnnotationBeanRemote aobj = (AnnotationBeanRemote) context.lookup("compute/AnnotationEJB/remote");
                //System.out.println("Annotation EJB lookup object successfully  ");
                
                if (aobj == null)
                        System.out.println("AnnotationBean is null");

                List<Entity> roots = aobj.getCommonRootEntitiesByTypeName("system", "Folder"); 

		System.out.println("*** BROWSING THE DATA OUTLINE - DISPLAY THE TOP MENU ***\n ");
		//System.out.println("Roots Length0: "+roots.size());

                assertNotNull(roots.size());

                for(Entity entity : roots) {
                        System.out.println("Processing "+entity.getName());
                        if (entity.getName().equals("FlyLight Single Neuron Samples")) {
                           System.out.println("!!!!!!!!!!  FlyLight Single Neuron Samples Folder exists  !!!!!!!!!!");

			        /**String type = null;
		                for (EntityData ed : entity.getEntityData()) {
                	        	Entity child = ed.getChildEntity();
                        		if (child!=null) {
                                		if (child.getName().equals("Whole Brain") && (type==null||type.equals(child.getEntityType().getName()))) {
							System.out.println("Entity Data: "+ed.getValue());
                                		}
                        		}
                		}**/
			}
                }

    	}catch (Exception e) {  
            	e.printStackTrace(); 
    	}
    }


    public static void main(String [] args) {
		AnnotationTreeRoot at = new AnnotationTreeRoot();
		at.testAnnotationTreeRoot();
		//at.testAnnotationTreeRoot("jacstest", "CC0ntrol!");
                 
    }
}
