
package org.janelia.it.jacs.compute;

import junit.framework.TestCase;
import org.apache.log4j.PropertyConfigurator;

import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 9, 2007
 * Time: 4:15:19 PM
 *
 */
public abstract class ComputeTestCase extends TestCase {

    public ComputeTestCase(String name) {
        super(name);
        configureLog4jConsole();
    }

    protected static void configureLog4jConsole() {
        Properties log4jprops = new Properties();
//        log4jprops.setProperty("log4j.rootCategory","ALL, stdout");
        log4jprops.setProperty("log4j.category.org.janelia.it.jacs.compute","ALL, stdout");
        log4jprops.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
        log4jprops.setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
        log4jprops.setProperty("log4j.appender.stdout.layout.ConversionPattern","%-5r[%24F:%-3L:%-5p]%x %m%n");
        PropertyConfigurator.configure(log4jprops);
    }

}
