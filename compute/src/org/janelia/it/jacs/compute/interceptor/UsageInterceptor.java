package org.janelia.it.jacs.compute.interceptor;

import org.apache.log4j.Logger;
import com.boxysystems.jgoogleanalytics.*;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

/**
 * Created by IntelliJ IDEA.
 * User: fosterl
 * Date: 5/31/12
 * Time: 11:06 AM
 * 
 * Records usage to a remote repository.
 */
public class UsageInterceptor {
    public static final String APPNAME_PREFIX = "Compute-";
    public static final String GA_CODE_PROP = "jacs.ga.code";

    // NOTE: IntelliJ may be false-erroring the session context injection.  Will be red-underscored.
    @Resource
    private SessionContext sessionContext;

    private Logger logger = Logger.getLogger( UsageInterceptor.class );
    private String serverName;
    private String jacsVersion;
    private String googleTrackingCode;
    private JGoogleAnalyticsTracker tracker;

    public UsageInterceptor() {
        SystemConfigurationProperties properties = SystemConfigurationProperties.getInstance();
        googleTrackingCode = properties.getProperty(GA_CODE_PROP);
        //todo consider refactoring ComputeBeanImpl to make these prop names more broadly accessible as constants.
        jacsVersion = properties.getProperty( "jacs.version" );
        serverName = properties.getProperty( "System.ServerName" );
        if ( logger.isDebugEnabled() ) {
            logger.debug(
                "Establishing a tracker with application=" + serverName +
                " version=" + jacsVersion +
                " and tracking code=" + googleTrackingCode
            );
        }
        tracker = new JGoogleAnalyticsTracker( APPNAME_PREFIX + serverName, jacsVersion, googleTrackingCode );
    }

    @AroundInvoke
    public Object intercept( InvocationContext ctx ) throws Exception {
        String targetMessage = ctx.getTarget().getClass().getSimpleName() + "." + ctx.getMethod().getName();
        if ( logger.isDebugEnabled() ) {
            logger.debug("Before invoking " + targetMessage);
        }

        // Focus point before....
        FocusPoint focusPoint = new FocusPoint( targetMessage );
        tracker.trackAsynchronously( focusPoint );

        Object rtnObj = ctx.proceed();

        // ...and again after.
        // NOTE: this apparently makes 2x usual tracking stats.
        //  tracker.trackAsynchronously( focusPoint );

        if ( logger.isDebugEnabled() ) {
            logger.debug( "After invoking " + ctx.getTarget().getClass().getSimpleName() + "." + ctx.getMethod().getName() );
        }
        return rtnObj;
    }
}
