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

<jsp:include page="/WEB-INF/jsp/common/HeaderProperties.jsp"/>
<!-- Load all properties -->
<jsp:include page="/WEB-INF/jsp/common/Properties.jsp"/>

<div id="content_box">
    <div id="gwt_header"></div>
    <center><div id="loadingMsg" class="loadingMsg">loading page...</div></center>
    <div id="page_wrapper">
        <div id="input-container"></div>
    </div>
    <iframe id="__gwt_historyFrame" style="width:0;height:0;border:0"></iframe>
</div>

<!--Footer will be shown after page contents load-->
<div id="footer" style="display:none"> 
    <jsp:include page="/WEB-INF/jsp/common/BasePageBottom.jsp"/>
</div>
