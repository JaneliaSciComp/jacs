
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
