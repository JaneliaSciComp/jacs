package org.janelia.it.jacs.compute.mbean;

import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.util.BulkEntityBuilder;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Short, synchronous, interactive tests of local bean interfaces. This is an area for prototyping and testing new 
 * functionality. These tests should later be formalized as unit tests. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Singleton
@Startup
@Remote(LocalTestManagerMBean.class)
public class LocalTestManager extends AbstractComponentMBean implements LocalTestManagerMBean {
	public LocalTestManager() {
		super("jacs");
	}

	private static final Logger logger = Logger.getLogger(LocalTestManager.class);
	
    public void testBulkLoadEntityTree(String ownerKey) throws Exception {
    	
    	BulkEntityBuilder builder = new BulkEntityBuilder(ownerKey);
    	
    	builder
    		.entity(EntityConstants.TYPE_FOLDER, "Bulk Load Entity Test")
    			.attr(EntityConstants.ATTRIBUTE_COMMON_ROOT, EntityConstants.ATTRIBUTE_COMMON_ROOT)
    			.entity(EntityConstants.TYPE_FOLDER, "Animalia")
    				.attr(EntityConstants.ATTRIBUTE_FILE_PATH, "/path/to/file")
    				.entity(EntityConstants.TYPE_FOLDER, "Chordata")
    					.entity(EntityConstants.TYPE_FOLDER, "Mammalia")
    						.entity(EntityConstants.TYPE_FOLDER, "Carnivora")
    							.entity(EntityConstants.TYPE_FOLDER, "Felidae")
    								.entity(EntityConstants.TYPE_FOLDER, "Panthera")
    									.entity(EntityConstants.TYPE_FILE, "Panthera Leo")
    									.up()
    								.up()
    							.up()
    						.up()
    					.up()
					.up()
					.entity(EntityConstants.TYPE_FOLDER, "Arthropoda")
						.entity(EntityConstants.TYPE_FOLDER, "Insecta")
							.entity(EntityConstants.TYPE_FOLDER, "Diptera")
								.entity(EntityConstants.TYPE_FOLDER, "Drosophilidae")
									.entity(EntityConstants.TYPE_FOLDER, "Drosophila")
										.attr(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE, "/groups/jacs/jacsShare/JaneliaWorkstation/JaneliaWorkstation_linux_1.5.3.0/workstation_128_icon.png")
									.up()
								.up()
							.up()
	    				.up()
    				.up()
    			.up();
    	
    	builder.entity(EntityConstants.TYPE_FOLDER, "Large Folder Test");
    	for(int i=0; i<1000; i++) {
    		builder.entity(EntityConstants.TYPE_FOLDER, "Sub folder "+i);
    		for(int j=0; j<1000; j++) {
    			builder.entity(EntityConstants.TYPE_FILE, "Nested file "+j);
    			builder.up();	
    		}
    		builder.up();
    	}
    	builder.up();
	
    	Entity root = builder.getRoot();
    			
    	logger.info("Saving tree '"+root.getName()+"'");
    	Entity saved = EJBFactory.getLocalEntityBean().saveBulkEntityTree(root);
    	logger.info("Saved tree '"+saved.getName()+"' as "+saved.getId());
    }    
}