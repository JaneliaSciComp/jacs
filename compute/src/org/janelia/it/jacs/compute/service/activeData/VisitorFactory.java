/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.service.activeData;

import org.janelia.it.jacs.compute.service.activeData.visitor.ActiveVisitor;

import java.util.Map;

/**
 *
 * @author murphys
 * @param <T>
 */
public class VisitorFactory <T extends ActiveVisitor> {
    Map<String, Object> parameterMap;
    Class<T> visitorType;
    
    public VisitorFactory(Map<String, Object> parameterMap, Class<T> visitorType) {
        this.parameterMap=parameterMap;
        this.visitorType=visitorType;
   }
    
   public T createInstance() throws Exception {
       T av = visitorType.newInstance();
       av.setParameterMap(parameterMap);
       return av;
   }
   
   public String getVisitorClassName() {
       return visitorType.getName();
   }
    
}
