/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.service.activeData.scanner;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;

import org.janelia.it.jacs.compute.service.activeData.ActiveDataScan;
import org.janelia.it.jacs.compute.service.activeData.VisitorFactory;

/**
 *
 * @author murphys
 */
public abstract class EntityScanner {
    
    private static final Logger logger = Logger.getLogger(EntityScanner.class);
    
    public static final String STATUS_PROCESSING = "Processing";
    public static final String STATUS_ERROR = "Error";
    public static final String STATUS_INACTIVE = "Inactive";
    public static final String STATUS_EPOCH_COMPLETED = "Epoch Completed";
    
    protected String status = STATUS_INACTIVE;
    boolean removeAfterEpoch=false;
    boolean transferPriorSuccess=true;
    
    private List<VisitorFactory> visitorFactoryList=new ArrayList<>();
    private String signature=null;
    
    public EntityScanner() {}
    
    public EntityScanner(List<VisitorFactory> visitorFactoryList) {
        this.visitorFactoryList=visitorFactoryList;
    }
       
    public abstract long[] generateIdList(Object dataResource) throws Exception;

    public void preEpoch(ActiveDataScan scan) throws Exception { return; }

    public void postEpoch(ActiveDataScan scan) throws Exception { return; }
    
    public List<VisitorFactory> getVisitorFactoryList() {
        return visitorFactoryList;
    }
    
    public String getSignature() {
        if (signature == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(this.getClass().getName());
            for (VisitorFactory vf : visitorFactoryList) {
                sb.append(":" + vf.getVisitorClassName());
            }
            signature = sb.toString();
        }
        return signature;
    }
    
    public void setStatus(String status) throws Exception {
        if (status==STATUS_PROCESSING ||
            status==STATUS_ERROR ||
            status==STATUS_INACTIVE ||
            status==STATUS_EPOCH_COMPLETED) {
            this.status=status;
        } else {
            throw new Exception("Do not recognize status type="+status);
        }
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setRemoveAfterEpoch(boolean removeAfterEpoch) {
        this.removeAfterEpoch=removeAfterEpoch;
    }
    
    public boolean getRemoveAfterEpoch() {
        return removeAfterEpoch;
    }

    public void setTransferPriorSuccess(boolean transferPriorSuccess) { this.transferPriorSuccess=transferPriorSuccess; }

    public boolean getTransferPriorSuccess() { return transferPriorSuccess; }
    
    protected long[] generateIdListByEntityType(Object dataResource, String entityTypeName) throws Exception {
      AnnotationBeanLocal annotationBeanLocal = EJBFactory.getLocalAnnotationBean();
        List<Long> allEntityIdsByType = null;
        try {
            allEntityIdsByType = annotationBeanLocal.getAllEntityIdsByType(entityTypeName);
        } catch (Exception ex) {
            logger.error(ex, ex);
            throw ex;
        }
        if (allEntityIdsByType == null || allEntityIdsByType.isEmpty()) {
            return new long[0];
        } else {
            long[] result = new long[allEntityIdsByType.size()];
            int i = 0;
            for (Long l : allEntityIdsByType) {
                result[i++] = l;
            }
            return result;
        }         
    }
    
}
