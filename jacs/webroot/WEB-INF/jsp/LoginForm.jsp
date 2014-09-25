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
    <head>
        <link rel="shortcut icon" href="/jacs/favicon.ico">
        <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1;"/>

        <jsp:include page="/WEB-INF/jsp/common/HeaderHead.jsp"/>
        <jsp:include page="/WEB-INF/jsp/common/Properties.jsp"/>
        
    </head>

    <body onLoad="if(document.forms[0].j_username != null) document.forms[0].j_username.focus()">
        <jsp:include page="/WEB-INF/jsp/common/HeaderBody.jsp"/>

        <!--<div id="page_wrapper">-->
        <div id="page_wrapper" style="display:none">
            <h1>Howard Hughes Medical Institute<br><i>Janelia Computational Services Login</i></h1>
            <p>(Same login/password as email)</p>

            <form method="POST" action="j_security_check">
                <div id="loginBox">
                    <table cellspacing="2" cellpadding="10" border="0">
                        <tr> <td>&nbsp;</td> <td>Username:</td> <td><input type="text" name="j_username"></td> <td>&nbsp;</td> </tr>
                        <tr> <td>&nbsp;</td> <td>Password:</td> <td><input type="password" name="j_password" value="nothing"></td> <td>&nbsp;</td> </tr>
                        <tr> <td>&nbsp;</td> <td>&nbsp;</td> <td style="text-align:center"><input type="submit" value="Login"></td> <td>&nbsp;</td> </tr>
                    </table>
                </div>
            </form>
        </div>
        <div id="footer" style="display:none">
            <jsp:include page="/WEB-INF/jsp/common/BasePageBottom.jsp"/>
        </div>
    </body>
    </html>
