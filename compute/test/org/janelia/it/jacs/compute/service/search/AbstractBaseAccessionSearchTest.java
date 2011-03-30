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

package org.janelia.it.jacs.compute.service.search;

import org.apache.log4j.PropertyConfigurator;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;

import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 12, 2007
 * Time: 1:01:36 PM
 *
 */
abstract public class AbstractBaseAccessionSearchTest extends AbstractTransactionalDataSourceSpringContextTests  {

    private SessionFactory sessionFactory;

    public AbstractBaseAccessionSearchTest(String name) {
        super(name);
        configureLog4jConsole();
    }

    protected static void configureLog4jConsole() {
        Properties log4jprops = new Properties();
        log4jprops.setProperty("log4j.rootCategory","ALL, stdout");
        log4jprops.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
        log4jprops.setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
        log4jprops.setProperty("log4j.appender.stdout.layout.ConversionPattern","%-5r[%24F:%-3L:%-5p]%x %m%n");
        PropertyConfigurator.configure(log4jprops);
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    protected String[] getConfigLocations() {
        return new String[] {
                "classpath*:/applicationContext-test.xml",
                "classpath*:/WEB-INF/applicationContext-common.xml"};
    }

    protected Session getCurrentSession() {
        return sessionFactory.getCurrentSession();
    }


}
