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

package org.janelia.it.jacs.compute.service.metageno;

import org.janelia.it.jacs.model.common.SystemConfigurationProperties;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 29, 2009
 * Time: 2:11:03 PM
 */
public class MetaGenoPerlConfig {

    public static String PERL5LIB = SystemConfigurationProperties.getString("MgPerl.PERL5LIB");
    public static String PERL_MOD_DIR = SystemConfigurationProperties.getString("MgPerl.PERL_MOD_DIR");
    public static String PERL_EXEC = SystemConfigurationProperties.getString("MgPerl.PERL_EXEC");
    public static String PERL_BIN_DIR = SystemConfigurationProperties.getString("MgPerl.PERL_BIN_DIR");
    public static String SYBASE = SystemConfigurationProperties.getString("MgPerl.SYBASE");

    public static String getCmdPrefix() {
        return getPerlEnvPrefix() +
                MetaGenoPerlConfig.PERL_EXEC + " " +
                MetaGenoPerlConfig.PERL_BIN_DIR + "/";
    }

    public static String getPerlEnvPrefix() {
        return "unset PERL5LIB\n" +
                "export PERL5LIB=" + MetaGenoPerlConfig.PERL5LIB + "\n" +
                "export PERL_MOD_DIR=" + MetaGenoPerlConfig.PERL_MOD_DIR + "\n" +
                "export SYBASE=" + MetaGenoPerlConfig.SYBASE + "\n";
    }

}
