/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.web.security;

import edu.sdsc.gama.services.SSO.GAMASSOFilter;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.spring.NonForgetfulPropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Nov 9, 2006
 * Time: 5:17:21 PM
 */
public class JacsGamaSsoFilter extends GAMASSOFilter {
    private Logger logger = Logger.getLogger(this.getClass());
    private Map<String, String> propMap = new HashMap<String, String>();
    Properties appProps = null;

    public JacsGamaSsoFilter() {
        // initialize map between original properties names and properties names from camera.preoperties
        propMap.put(SSO_COOKIE_NAME_PARAM, "gama.sso.cookie");
        propMap.put(SSO_LOGIN_PAGE_PARAM, "gama.sso.loginpage");
        propMap.put(SSO_FILE_DIRECTORY_PARAM, "gama.sso.filedirectory");
        propMap.put(SSO_DEBUG_PARAM, "gama.sso.debug");

    }

    protected String getPropertyValue(String propName) {
        if (appProps == null) {
            ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(getFilterConfig().getServletContext());
            // name of the properties bean must be the same as in appContext xml descriptor
            appProps = ((NonForgetfulPropertyPlaceholderConfigurer) ctx.getBean("propertyConfigurer")).getProps();
        }
        String value = appProps.getProperty(propMap.get(propName));
        if (StringUtils.hasText(value))
            return value;
        else
            return super.getPropertyValue(propName);    //To change body of overridden methods use File | Settings | File Templates.
    }
    // override parents output using log4j

    protected void debugOut(String message) {
        if (isDebug())
            logger.debug(message);
    }

    protected void infoOut(String message) {
        logger.info(message);
    }

    protected void debugErr(String message, Throwable e) {
        logger.error(message, e);
    }

}
