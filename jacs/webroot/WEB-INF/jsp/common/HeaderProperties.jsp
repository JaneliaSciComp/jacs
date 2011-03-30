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

<%
    // Get a handle to the properties
    ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(request.getSession().getServletContext());
    Properties properties = ((NonForgetfulPropertyPlaceholderConfigurer) ctx.getBean("propertyConfigurer")).getProps();
%>
    <!-- Accrue the values needed for the header.  GWT can access this JavaScript array. -->
    <script type="text/javascript">
        var headerValues = new Array();
        headerValues["HomeURL"]     = "<%=properties.getProperty("Header.HomeURL")    %>";
        headerValues["HomeAboutURL"]     = "<%=properties.getProperty("Header.HomeAboutURL")    %>";
        headerValues["HomeEducationURL"] = "<%=properties.getProperty("Header.HomeEducationURL")    %>";
        headerValues["HomeCommunityURL"] = "<%=properties.getProperty("Header.HomeCommunityURL")    %>";
        headerValues["LoginURL"]    = "<%=properties.getProperty("gama.sso.loginpage")%>";
        headerValues["LogoutURL"]   = "<%=properties.getProperty("Header.LogOutLink") %>";
        headerValues["RegisterURL"] = "<%=properties.getProperty("Header.RegisterURL")%>";
        headerValues["ContactURL"]  = "<%=properties.getProperty("Header.ContactURL") %>";
        headerValues["HelpURL"]     = "<%=properties.getProperty("Header.HelpURL")    %>";
    </script>
