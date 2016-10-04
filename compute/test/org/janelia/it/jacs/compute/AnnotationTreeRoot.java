package org.janelia.it.jacs.compute;

import org.janelia.it.jacs.compute.api.AnnotationBeanRemote;
import org.janelia.it.jacs.model.entity.Entity;
import org.junit.Test;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;
import java.util.List;

import static junit.framework.Assert.assertNotNull;

/****
	author:	Anitha Parvatham
	date  : 06/27/2012
****/

public class AnnotationTreeRoot {

    @Test
    public void testAnnotationTreeRoot() {
    try {  
    		Hashtable environment = new Hashtable();
        	environment.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
        	environment.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        	environment.put(Context.PROVIDER_URL, "remote://jacs-staging:1199");
        	InitialContext context = new InitialContext(environment);
        	//System.out.println("-->> connected successfully to server");

                //System.out.println("\n*************************************\n"); 

        	AnnotationBeanRemote aobj = (AnnotationBeanRemote) context.lookup("compute/AnnotationEJB/remote");
                //System.out.println("Annotation EJB lookup object successfully  ");
                
                if (aobj == null)
                        System.out.println("AnnotationBean is null");

                List<Entity> roots = aobj.getCommonRootEntities("system");

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
                                		if (child.getName().equals("Whole Brain") && (type==null||type.equals(child.getEntityTypeName()))) {
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
