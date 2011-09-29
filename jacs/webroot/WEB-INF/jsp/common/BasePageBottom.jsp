<%@ page import="org.janelia.it.jacs.spring.AppVersionResolver" %>
<%@ page import="org.janelia.it.jacs.spring.NonForgetfulPropertyPlaceholderConfigurer" %>
<%@ page import="org.springframework.context.ApplicationContext" %>
<%@ page import="org.springframework.web.context.support.WebApplicationContextUtils" %>
<%@ page import="java.util.Properties" %>



<%--
  ~ Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
  ~
  ~ This file is part of JCVI VICS.
  ~
  ~ JCVI VICS is free software; you can redistribute it and/or modify it
  ~ under the terms and conditions of the Artistic License 2.0.  For
  ~ details, see the full text of the license in the file LICENSE.txt.  No
  ~ other rights are granted.  Any and all third party software rights to
  ~ remain with the original developer.
  ~
  ~ JCVI VICS is distributed in the hope that it will be useful in
  ~ bioinformatics applications, but it is provided "AS IS" and WITHOUT
  ~ ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
  ~ implied warranties of merchantability or fitness for any particular
  ~ purpose.  For details, see the full text of the license in the file
  ~ LICENSE.txt.
  ~
  ~ You should have received a copy of the Artistic License 2.0 along with
  ~ JCVI VICS.  If not, the license can be obtained from
  ~ "http://www.perlfoundation.org/artistic_license_2_0."
  --%>

<script type=text/javascript>
    function popUpDisclaimer(url) {
       window.open(url,
                   "Disclaimer",
                   "status=0, " +
                   "toolbar=0, " +
                   "location=0, " +
                   "menubar=0, " +
                   "directories=0, " +
                   "scrollbars=0, " +
                   "resizable=0, " +
                   "height=500, " +
                   "width=500");
    }
</script>

<%
    ApplicationContext ctx =
            WebApplicationContextUtils.getWebApplicationContext(request.getSession().getServletContext());

    // Get a handle to the properties
    Properties properties = ((NonForgetfulPropertyPlaceholderConfigurer) ctx.getBean("propertyConfigurer")).getProps();
    String googleAnalyticsId=properties.getProperty("GoogleAnalytics.ComputeServer.Id");

    // get version. If deployed internally - use full version number
    int deploymentCtx;
    try
    {
        deploymentCtx = Integer.parseInt(properties.getProperty("TimebasedIdentifierGenerator.DeploymentContextNumber"));
    }
    catch (NumberFormatException e)
    {
        deploymentCtx = 12; // default to production env
    }

    AppVersionResolver appVersionResolver = (AppVersionResolver) ctx.getBean("appVersionResolver");
    String appVersion = appVersionResolver.getAppVersion(true); // get full version for dev and integration
//    String appVersion = appVersionResolver.getAppVersion(deploymentCtx < 4); // get full version for dev and integration
    String helpdesk = properties.getProperty("System.ErrorMessageDestination");
%>

  <table align="center" cellpadding="0" cellspacing="0" border="0">
   <tr>
     <td>
         <%-- <a class="smallFooterLink"
            href="mailto:saffordt@janelia.hhmi.org">
            Help desk</a> --%>
         <%-- <a class="smallFooterLink"
            href="mailto:saffordt@janelia.hhmi.org">
            Help desk</a> --%>
         <a class="smallFooterLink"
            href="mailto:<%=helpdesk%>">
            Help desk</a>

       <span class="smallLinkSeparator">&nbsp;|&nbsp;</span>

       <a class="smallFooterLink"
          onclick="popUpDisclaimer('<%=request.getContextPath()%>/disclaimer.htm')">
          Disclaimer</a>
         
       <span class="smallLinkSeparator">&nbsp;|&nbsp;</span><span class="smallLinkSeparator">Version <%=appVersion%></span>
       <span class="smallLinkSeparator">&nbsp;|&nbsp;</span><span class="smallLinkSeparator">Deployment Ctx is <%=deploymentCtx%></span>
     </td>
   </tr>
  </table>

<!-- experimental code for Google Analytics -->
<%-- for now - disable google analytics --%>
<%--<script type="text/javascript">--%>
    <%--var gaJsHost = (("https:" == document.location.protocol) ? "https://ssl." : "http://www.");--%>
    <%--document.write(unescape("%3Cscript src='" + gaJsHost + "google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E"));--%>
<%--</script>--%>
<%--<script type="text/javascript">--%>
    <%--var pageTracker = _gat._getTracker("<%=googleAnalyticsId%>");--%>
    <%--pageTracker._initData();--%>
    <%--pageTracker._trackPageview();--%>
<%--</script>--%>

<%--<script src="http://www.google-analytics.com/urchin.js" type="text/javascript">--%>
<%--</script>--%>
<%--<script type="text/javascript">--%>
<%--_uacct = "<%=googleAnalyticsId%>";--%>
<%--urchinTracker();--%>
<%--</script>--%>
