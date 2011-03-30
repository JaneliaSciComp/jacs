<%@ page contentType="text/html;charset=UTF-8" language="java" %>
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

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
    <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
    <head>
        <title>JaCS Error 500</title>

        <!-- Load CSS -->
        <link type="text/css" rel="stylesheet" href="/jacs/css/jacs.css"/>

        <!-- Load FavIcon -->
        <link rel="shortcut icon" href="/jacs/favicon.ico"/>
        <jsp:include page="/WEB-INF/jsp/common/HeaderHead.jsp"/>
    </head>
    <body>
        <jsp:include page="/WEB-INF/jsp/common/HeaderBody.jsp"/>
        <div id="page_wrapper" style="display:none">
            <p/><span class="jspTitle">${exceptionBean.message}.</span>
            <p/><span class="text">Sorry, this feature is not yet implemented.</span>
            <p/><span class="text">HOWEVER, you can still download this sequence on the preceding page, just select the subject sequence of interest from the list and hit "export sequence".
            This allows you to download subject sequences in FASTA format.</span>
        </div>
        <div id="footer" style="display:none">
            <jsp:include page="/WEB-INF/jsp/common/BasePageBottom.jsp"/>
        </div>
    </body>
</html>
