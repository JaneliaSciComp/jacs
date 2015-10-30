
package org.janelia.it.jacs.test;

import org.apache.log4j.PropertyConfigurator;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;

import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 6, 2007
 * Time: 2:05:36 PM
 *
 */
public abstract class JacswebTestCase extends AbstractTransactionalDataSourceSpringContextTests {

    public JacswebTestCase(String name) {
        super(name);
        configureLog4jConsole();
    }

    abstract protected class TimedTest {
        protected static final int IDLE = 0;
        protected static final int STARTED = 1;
        protected static final int RUNNING = 2;
        protected static final int FINISHED = 3;
        protected static final int TIMEDOUT = 4;
        protected int testStatus;
        protected Object testResult;
        protected Exception testException;
        protected TimedTest() {
        }

        abstract protected Object invokeTest(Object... testParams) throws Exception;

        public Object runTest(String testName,long timeout,final Object... testParams) throws Exception {
            if(timeout > 0) {
                testStatus = STARTED;
                testResult = null;
                testException = null;
                // spin off a new thread
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            testStatus = RUNNING;
                            testResult = invokeTest(testParams);
                        } catch(Exception e) {
                            testException = e;
                        }
                        testStatus = FINISHED;
                    }
                },testName).start();
                long startTime = System.currentTimeMillis();
                boolean timeoutFlag = false;
                // and in the current thread check the test's status
                while(true) {
                    if(testStatus == FINISHED) {
                        break;
                    } else if(testStatus == RUNNING) {
                        if(System.currentTimeMillis() - startTime > timeout) {
                            timeoutFlag = true;
                            break;
                        }
                    }
                    Thread.sleep(500);
                }
                assertFalse("Test " + testName + " timed out",timeoutFlag);
                if(testException != null) {
                    throw testException;
                }
            } else {
                testStatus = RUNNING;
                testResult = invokeTest(testParams);
                testStatus = FINISHED;
            }
            return testResult;
        }

    }

    protected String[] getConfigLocations() {
        return new String[] {
                "classpath*:/WEB-INF/applicationContext-test.xml",
                "classpath*:/WEB-INF/applicationContext-common.xml"};
    }

    protected static void configureLog4jConsole() {
        Properties log4jprops = new Properties();
        log4jprops.setProperty("log4j.rootCategory","ERROR, stdout");
        log4jprops.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
        log4jprops.setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
        log4jprops.setProperty("log4j.appender.stdout.layout.ConversionPattern","%-5r[%24F:%-3L:%-5p]%x %m%n");
        PropertyConfigurator.configure(log4jprops);
    }

    protected void failFromException(Exception ex) {
        String message="Exception: "+ex.getMessage();
        logger.warn(message);
        fail(message);
    }
}
