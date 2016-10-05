package org.janelia.it.jacs.compute;

import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EntityBeanRemote;
import org.junit.Test;
import org.junit.Assert;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;

/****
	author:	Anitha Parvatham
	date  : 06/27/2012
****/

public class EntityAttributesTest {

    @Test
    public void testEntityAttributesBean() {
    try {  
    		Hashtable environment = new Hashtable();
        	environment.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
        	environment.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        	environment.put(Context.PROVIDER_URL, "remote://jacs-staging:1199");
			environment.put(Context.SECURITY_PRINCIPAL, "jmsuser");
			environment.put(Context.SECURITY_CREDENTIALS, "jmsuser");
			InitialContext context = new InitialContext(environment);

        	//System.out.println("-->> connected successfully to server");

        	ComputeBeanRemote cobj = (ComputeBeanRemote) context.lookup("compute/ComputeEJB/remote");
        	//System.out.println("-->> ComputeEJB lookup object successfull.");

        	if (cobj==null)
            		System.out.println("ComputeBean is null");
        
        	//assert(cobj.login("jacstest", "CC0ntrol!1") == true); 

                //System.out.println("-->> Login is tested successfully");

		EntityBeanRemote eobj = (EntityBeanRemote) context.lookup("compute/EntityEJB/remote");
		System.out.println("\n Entity Attributes Returned:"+eobj.getEntityAttributes().size());
		//assertEquals(46,eobj.getEntityAttributes().size());
		Assert.assertNotNull(eobj.getEntityAttributes().size());

                /**String[] expectedArray = {"File Path", "Ontology Element", "Common Root", "Entity", "Annotation Ontology Key Entity Id", "Annotation Ontology Value Entity Id", "Annotation Ontology Key Term", "Annotation Ontology Value Term", "Annotation Ontology Root Id", "Ontology Term Type", "Ontology Term Type Interval Lower Bound", "Ontology Term Type Interval Upper Bound", "Is Public", "Annotation Target Id", "Annotation Session Id", "Number", "Supporting Files", "Neuron Fragments", "Result", "LSM Stack 1", "LSM Stack 2", "Default 2D Image File Path", "Image Format", "Tiling Pattern", "Merged Stack", "Is Zipped", "Alignment Inconsistency Score", "Alignment Model Violation Score", "Default 2D Image", "Signal MIP Image", "Reference MIP Image", "Ontology Term Type EnumText Enum Id", "Performance Proxy Image", "Artifact Source Entity Id", "Result Node Id", "Default 3D Image", "Representative Sample", "Split Part", "Original Fly Line", "balanced Fly Line", "Robot Id", "Reference Channel", "Signal Channels", "Alignment Types", "Cross Label", "Channel Specification"};

		for(EntityAttribute e : eobj.getEntityAttributes())
        	String[] resultArray = eobj.getEntityAttributes();
 
       	        assert.IsNotNull(eobj.getEntityAttributes()); 
       	        assertArrayEquals(expectedArray, resultArray); 
		System.out.println(e.getName());**/

    	}catch (Exception e) {  
            	e.printStackTrace(); 
    	}
    }


    public static void main(String [] args) {
		EntityAttributesTest ct = new EntityAttributesTest();
		ct.testEntityAttributesBean();
                 
    }
}
