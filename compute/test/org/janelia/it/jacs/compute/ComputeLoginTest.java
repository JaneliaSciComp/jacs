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
//import org.janelia.it.jacs.shared.utils.*;
import org.junit.*;
import static org.junit.Assert.*;

/****
	author:	Anitha Parvatham
	date  : 06/27/2012
****/

public class ComputeLoginTest {

    //ComputeBeanRemote computeBean;
    @Test
    public void testComputeLoginBean() {
    try {  
    		Hashtable environment = new Hashtable();
        	environment.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
        	environment.put(Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces");
        	environment.put(Context.PROVIDER_URL, "jnp://jacs-staging:1199"); 
        	InitialContext context = new InitialContext(environment);

        	//System.out.println("-->> connected successfully to server");

        	ComputeBeanRemote cobj = (ComputeBeanRemote) context.lookup("compute/ComputeEJB/remote");
        	//System.out.println("-->> ComputeEJB lookup object successfull.");

        	if (cobj==null)
            		System.out.println("ComputeBean is null");
        
        	assert(cobj.login("jacstest", "CC0ntrol!1") == true); 

                System.out.println("\n LOGIN IS TESTED SUCCESSFULYY ");

		EntityBeanRemote eobj = (EntityBeanRemote) context.lookup("compute/EntityEJB/remote");
		//System.out.println("--> Remote entity attributes returned:"+eobj.getEntityAttributes().size());
		// assertEquals(49,eobj.getEntityAttributes().size());

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
		ComputeLoginTest ct = new ComputeLoginTest();
		ct.testComputeLoginBean();
                 
    }
}
