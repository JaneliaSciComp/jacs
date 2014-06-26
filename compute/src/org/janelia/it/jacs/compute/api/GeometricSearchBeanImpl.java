/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.api;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import org.apache.log4j.Logger;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;
import org.jboss.ejb3.StrictMaxPool;

/**
 *
 * @author murphys
 */
@Stateless(name = "GeometricSearchEJB")
@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
@TransactionTimeout(432000)
//@Interceptors({UsageInterceptor.class})
@PoolClass(value = StrictMaxPool.class, maxSize = 100, timeout = 10000)
public class GeometricSearchBeanImpl implements GeometricSearchBeanLocal, GeometricSearchBeanRemote {
	
    private static final Logger _logger = Logger.getLogger(GeometricSearchBeanImpl.class);
    
}
