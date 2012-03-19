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

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <title>Formatting Content for Project or Publication Pages</title>

    <!-- Load CSS -->
    <link type="text/css" rel="stylesheet" href="/jacs/css/styleTutorial.css"/>


</head>

<body>

<!--
<a href="/wiki/display/VISW/Formatting+Content+for+Project+or+Paper+Pages?decorator=printable" rel="nofollow"><img src="/wiki/images/icons/print_16.gif" width="16" height="16" hspace="1" vspace="1" align="absmiddle" border="0" alt="View a printable version of the current page." title="View a printable version of the current page."/></a>
<img src="/wiki/images/icons/attachments/pdf.gif" height="16" width="16" border="0" align="absmiddle" title="Export Page as PDF"></a>

<div class="logoSpaceLink" style="margin-top: 5px; margin-bottom:5px">            <a href="/wiki/display/VISW">JaCS</a>    </div>
<div class="pagetitle" style="padding: 0px; margin-bottom:5px; margin-top: 2px;">
Formatting Content for Project or Paper Pages
</div>
-->

<h1>Formatting Content for Project or Paper Pages</h1>


<div class="wiki-content" style="margin-right:10px;">
<h3><a name="FormattingContentforProjectorPaperPages-Introduction"></a>Introduction</h3>
<p>The JaCS "research site" hosts project, project data and project publications for marine metagenomics-related projects. Each project and publication has an information page that displays formatted HTML describing the project or publication.&nbsp; This formatted HTML can be submitted by the project staff or publication author(s) to the JaCS project for inclusion in the application.&nbsp;</p>
<p>This page describes how you can format your HTML using the CSS styles in use by the JaCS application.</p>
<p>Note that some project and publication information is put on the screen automatically by the application.&nbsp; Each project page displays the basic project information (project name, principal investigator, institutional affiliation, etc. ) above your custom HTML.&nbsp; Similarly, each publication page automatically displays the publication title above your custom HTML.</p>
<p>See the attached <b><span class="nobr"><a href="/wiki/download/attachments/7978/project.jpg?version=1">project.jpg<sup><img class="rendericon" src="/wiki/images/icons/link_attachment_7.gif" height="7" width="7" align="absmiddle" alt="" border="0"/></sup></a></span></b> and <b><span class="nobr"><a href="/wiki/download/attachments/7978/publication.jpg?version=1">publication.jpg<sup><img class="rendericon" src="/wiki/images/icons/link_attachment_7.gif" height="7" width="7" align="absmiddle" alt="" border="0"/></sup></a></span></b> screen shots for reference.</p>

<br>
<h3><a name="FormattingContentforProjectorPaperPages-BasicHTMLTextandImageFormatting"></a>Basic HTML Text and Image Formatting</h3>

</div>

<p>Basic Text formatting:</p>
<ul>
	<li><ins>The entire page</ins> should be wrapped using the "text" style: <font color="red">&lt;div class="text"&gt;</font> (text goes here) <font color="red">&lt;/div&gt;</font></li>
</ul>


<ul>
	<li><ins>Each paragraph</ins> should be wrapped with "&lt;p&gt;" and "&lt;/p&gt;" tags: <font color="red">&lt;p&gt;</font> (paragraph goes here) <font color="red">&lt;/p&gt;</font></li>
	<li><ins>Superscripts</ins> can be formatted using the "superscript" style:&nbsp;&nbsp; 5 x 10<font color="red">&lt;span class="superscript"&gt;</font><font color="black">5</font><font color="red">&lt;/span&gt;</font></li>
	<li><ins>Prompts</ins> can be added using the "prompt" style, like this: <font color="red">&lt;span class="prompt"&gt;</font><font color="black">Abstract:</font> <font color="red">&lt;/span&gt;</font></li>
	<li><ins>External links</ins> (links to web pages outside the JaCS application, such as citation links) are displayed with a double-underline and open in a new window.&nbsp; Use the "externalLinkText" style and the target="_blank" attribute : <font color="red">&lt;a class="externalLinkText" target="_blank" href="www.google.com"           &gt;</font>External link to Google<font color="red">&lt;/a&gt;</font></li>
</ul>


<p>Adding images:</p>
<ul>
	<li>Images can be included in the HTML, but must use URLs to reference the image source (i.e. the image must be available on the Internet somewhere).</li>
	<li>Place an image between paragraphs, and "float" it to the left (which will wrap the text around the image) using the "floatImg" style on the &lt;img&gt; tag, like this: &nbsp;<font color="red">&lt;img class="floatImg" src="http://somedomain/someimage.jpg</font><font color="red">" height="123"  width="123"&gt;</font>. Hard-code the image size using the height and width attributes if you want it smaller than the original.&nbsp; About 350 pixels wide looks right, so adjust the height to the right value so the image scales properly.</li>
</ul>


<h3><a name="FormattingContentforProjectorPaperPages-AdvancedImageFormatting"></a>Advanced Image Formatting</h3>

<p>You can add a caption to your image, and make the image function as a hyperlink so that when the user clicks on the image, a new window pops up with a larger version of the image or another web page for more information.&nbsp; Use this block of code for each image:</p>
<ul>
	<li><div class="panel" style="border-style: solid borderColor=#ccc; "><div class="panelHeader" style="border-bottom-style: solid borderColor=#ccc; background-color: #EFEFEF; "><b>Hyperlinked image with centered caption</b></div><div class="panelContent" style="background-color: #EFEFEF; ">
<p>&lt;div class="floatImg" style="width:<font color="red"><b>316px</b></font><font color="red">"</font>&gt;<br/>
&nbsp;&nbsp;&nbsp;&lt;a href="<font color="green"><b>More-info-URL</b></font>" target="blank"&gt;<br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &lt;img class="linkedImage" src="<font color="green"><b>Image-URL</b></font>"width="<font color="red"><b>316</b></font>" height="<font color="green"><b>196</b></font>"&gt;<br/>
&nbsp;&nbsp;&nbsp;&lt;/a&gt;<br/>
&nbsp;&nbsp;&nbsp;&lt;br/&gt;<br/>
&nbsp;&nbsp;&nbsp;&lt;span class="caption"&gt;<font color="green"><b>Figure X: Caption goes here</b></font>&lt;/span&gt;<br/>
&nbsp;&nbsp;&nbsp;&lt;br/&gt;<br/>
&nbsp;&nbsp;&nbsp;&lt;span class="hint"&gt;Click on the image for more information.&lt;/span&gt;<br/>
&lt;/div&gt;
<br clear="all" />  <font color="green"><b>Green</b></font>: fill in your info here.<br/>
<font color="red"><b>Red</b></font><font color="red">:</font> fill in these values too, but note that the width of the div must match the image width so the caption wraps properly; you <b>must</b> append the "px" to the div width and <b>must not</b> to the image width.</p>
</div></div><br clear="all" /> <br clear="all" />
<br clear="all" /></li>
</ul>


<h3><a name="FormattingContentforProjectorPaperPages-TestingYourHTML%3A"></a>Testing Your HTML:</h3>

<p>You can test your HTML locally to make sure the formatting will look right within the JaCS application:</p>
<ol>
	<li>Save the attached <b><span class="nobr"><a href="/wiki/download/attachments/7978/template.html?version=1">template.html<sup><img class="rendericon" src="/wiki/images/icons/link_attachment_7.gif" height="7" width="7" align="absmiddle" alt="" border="0"/></sup></a></span></b> to your file system and view the file  in your browser (use "File-&gt;Open.." or "File-&gt;Open File").</li>
	<li>Open the file for editing, and insert your formatted text where it says to.&nbsp;</li>
</ol>


<p>Note that the  formatted HTML that you will submit should not include the stuff in the template  (i.e. don't submit the &lt;html&gt; tags or the &lt;style&gt; stuff, that's just for local testing).</p>

<h3><a name="FormattingContentforProjectorPaperPages-ExampleHTMLBasicFormatting"></a>Example HTML - Basic Formatting</h3>

<p>
    <br clear="all" />
<div class="preformatted"><div class="preformattedContent">
<pre>&lt;div class="text"&gt;
	&lt;p&gt;&lt;span class="prompt"&gt;Abstract: &lt;/span&gt; Very long paragraph of abstract goes here.&lt;/p&gt;
	&lt;img class="floatImg" src="http://some.url" height="11"  width="27"&gt;
	&lt;p&gt;Another paragraph of text goes here; it will wrap around the image.&lt;/p&gt;
&lt;/div&gt;
</pre>
</div></div>
    <br clear="all" />
    <br clear="all" />
    <br clear="all" />
</p>

<h3><a name="FormattingContentforProjectorPaperPages-ExampleHTMLAdvancedFormatting%26nbsp%3B"></a>Example HTML - Advanced Formatting&nbsp;</h3>

<p>See the attached <b><span class="nobr"><a href="#forestHtmlExample">forest.html<sup><img class="rendericon" src="/wiki/images/icons/link_attachment_7.gif" height="7" width="7" align="absmiddle" alt="" border="0"/></sup></a></span></b>
    <b>,</b> which contains the HTML (and the template HTML) used to generate the attached <b><span class="nobr"><a href="#forestScreenshot">publication.jpg</a></span></b> screen shot.<br clear="all" />
    <br clear="all" />
<div class="panel" style="border-style: solid borderColor=#ccc; ">
<div class="panelHeader" style="border-bottom-style: solid borderColor=#ccc; background-color: #EFEFEF; ">
    <b id="forestHtmlExample">forest.html example</b></div>

<div class="panelContent" style="background-color: #EFEFEF; ">
<pre xml:space="preserve">
&lt;html&gt; &lt;head&gt; &lt;/head&gt; &lt;body&gt;
    &lt;div class="text"&gt;
    &lt;span class="prompt"&gt;Citation: &lt;/span&gt;
    &lt;a class="externalTextLink" href="http://dx.doi.org/10.1371/journal.pbio.0040368" target="_blank"&gt;The Marine Viromes of Four Oceanic Regions&lt;/a&gt;
    &lt;p/&gt;Angly FE,&nbsp;Felts B,&nbsp;Breitbart M,&nbsp;Salamon P,&nbsp;Edwards RA, et al.
    &lt;br/&gt;PLoS Biology Vol. 4, No. 11, e368 doi:10.1371/journal.pbio.0040368

    &lt;p&gt;&lt;span class="prompt"&gt;Abstract: &lt;/span&gt; Viruses are the most common biological entities in the marine environment. Most marine viruses are phages (bacteriophages) that kill the heterotrophic and autotrophic microbes (both Bacteria and presumably Archaea) that dominate the world's oceans. Phages and the other major microbial predator guild, nanoflagellates, control the numbers of marine microbes to a concentration of about 5 x 10&lt;span class="superscript"&gt;5&lt;/span&gt; cells per ml of surface seawater.&lt;/p&gt;
    &lt;div class="floatImg" style="width:316"&gt;
        &lt;a href="http://biology.plosjournals.org/perlserv/?request=slideshow&amp;type=figure&amp;doi=10.1371/journal.pbio.0040368&amp;id=65945" target="blank"&gt;
             &lt;img class="linkedImage" src="http://biology.plosjournals.org/archive/1545-7885/4/11/figure/10.1371_journal.pbio.0040368.g006-L.jpg" height="196" width="316"&gt;
        &lt;/a&gt;
        &lt;br&gt;
        &lt;span class="caption"&gt;Figure 2. Composition of the Assemblage Genome Sequences as Determined by Similarity to Known DNA and Protein Sequences.&lt;/span&gt;
        &lt;br/&gt;
        &lt;span class="hint"&gt;Click on the image for more information.&lt;/span&gt;
    &lt;/div&gt;
    &lt;p&gt;Phages affect microbial evolution by inserting themselves into genomes as prophages. Prophages often account for most of the differences between strains of the same microbial species, and they can dramatically change the phenotype of the hosts via lysogenic conversion. For example, many nonpathogens and pathogens only differ by prophages that encode exotoxin genes. Phages also affect microbial evolution by moving genes from host to host. It has been hypothesized that most of the orphan open reading frames (ORFans) in microbial genomes are actually of phage origin. Phages may also affect microbial evolution by killing specific microbes. A type of Lotka-Volterra models, called "kill-the-winner," predict that as one microbial strain becomes dominant, its viral predator kills it and leaves open a niche that can be used by a related strain that is resistant to the phage. This model may explain the enormous microdiversity observed in microbial communities.&lt;/p&gt;
    &lt;p&gt;There has not been a global survey of marine viruses, and consequently, it is not known what types of viruses are in Earth's oceans or how they are distributed. Is the enormous viral diversity explained by a strong local selection, making every marine location a unique habitat? Or are marine currents and sea breezes responsible for a global spreading, homogenizing viral assemblages and supporting the observation of identical phages in different locations. Metagenomic analyses of viral samples from different provinces can help us answer these questions.&lt;/p&gt;
    &lt;p&gt;184 viral assemblages collected over a decade and representing 68 sites in four major oceanic regions (Sargasso Sea, Coast of British Columbia, Gulf of Mexico and Arctic Ocean) showed that most of the viral sequences were not similar to those in the current databases. There was a distinct "marine-ness" quality to the viral assemblages. Global diversity was very high, presumably several hundred thousand of species, and regional richness varied on a North-South latitudinal gradient. The marine regions had different assemblages of viruses.&lt;/p&gt;
    &lt;div class="floatImg" style="width:316"&gt;
        &lt;a href="http://biology.plosjournals.org/perlserv/?request=slideshow&amp;type=figure&amp;doi=10.1371/journal.pbio.0040368&amp;id=65929" target="blank"&gt;
             &lt;img class="linkedImage" src="http://biology.plosjournals.org/archive/1545-7885/4/11/figure/10.1371_journal.pbio.0040368.g002-L.jpg" height="196" width="316"&gt;
        &lt;/a&gt;
        &lt;br&gt;
        &lt;span class="caption"&gt;Figure 2. Composition of the Assemblage Genome Sequences as Determined by Similarity to Known DNA and Protein Sequences&lt;/span&gt;
        &lt;br/&gt;
        &lt;span class="hint"&gt;Click on the image for more information.&lt;/span&gt;
    &lt;/div&gt;
    &lt;p&gt;Cyanophages and a newly discovered clade of single-stranded DNA phages dominated the Sargasso Sea sample, whereas prophage-like sequences were most common in the Arctic. However most viral species were found to be widespread. With a majority of shared species between oceanic regions, most of the differences between viral assemblages seemed to be explained by variation in the occurrence of the most common viral species and not by exclusion of different viral genomes. These results support the idea that viruses are widely dispersed and that local environmental conditions enrich for certain viral types through selective pressure.&lt;/p&gt;
    &lt;p&gt;&lt;span class="prompt"&gt;Synopsis and Key Data Sets: &lt;/span&gt; The advent of whole-community genome sequencing (i.e., metagenomics) is rapidly changing the way viral and microbial diversity are assayed. Using this approach, it is possible to rapidly characterize the metabolic diversity and community structure of any microbial ecosystem. We studied the marine viral metagenome (virome) of four oceanic regions. The viromes were obtained by pyrosequencing uncultured viral assemblages that were integrated over 4,600 km in distance, 3,000 m in depth, and over a decade in time in order to characterize them and identify patterns of viral distribution and diversity. Samples were collected from the Sargasso Sea, the Arctic Ocean, the Bay of British Columbia, and the Gulf of Mexico. The viral DNA from each region was pooled together, and sequenced, resulting in four collections of sequences, totalling 1.8 million sequences, including significant representation from viral (14.2%), prophage (12.1%), bacterial (72.5%), archael(0.2%), and eukaryotic (1.1%) sequences. The data from the Sargasso Sea contained over 84% viral and prophage sequences.&lt;/p&gt;
    &lt;p&gt;&lt;/p&gt;&lt;p&gt;In addition to the metagenomic sequences, the authors provide the consensus sequences of a chp1-like microphage and a chlamydia phage (AY769964.1).
    &lt;/p&gt;
&lt;/body&gt; &lt;/html&gt;
</pre>
</div></div>
<br clear="all" />


<div class="panel" style="border-style: solid borderColor=#ccc; ">
<div class="panelHeader" style="border-bottom-style: solid borderColor=#ccc; background-color: #EFEFEF; ">
<b id="forestScreenshot">forest.html screenshot (publication.jpg) </b></div>

<div class="panelContent" style="background-color: #EFEFEF; ">
<img src="/jacs/images/publication2.jpg">
</div>
</div>











<h3><a name="FormattingContentforProjectorPaperPages-LinkingGraphicsfromPLoSBiology"></a>Linking Graphics from PLoS Biology</h3>

<p><font color="black">1) To link to the first image in a  slideshow</font>, <font color="black">you can use information  including</font>&#42; <font color="black">journal name</font> <font color="#400040">(biology</font>, <font color="#400040">medicine</font>, <font color="#400040">compbio</font>, <font color="#400040">genetics</font>, <font color="#400040">pathogens</font>, <font color="#400040">clinicaltrials)</font></p>
<ul>
	<li><font color="black">image type (figure</font>, <font color="black">table</font>, <font color="black">media)</font></li>
	<li><font color="black">article DOI</font></li>
</ul>


<p><font color="black">&nbsp;For example</font>, where"  rel="nofollow"linktype="raw" linktext="http://biology.plosjournals.org/perlserv/?request=slideshow&amp;type=figure&amp;doi=10.1371/journal.pbio.0040368where"&gt;<span class="nobr"><a href="http://biology.plosjournals.org/perlserv/?request=slideshow&amp;type=figure&amp;doi=10.1371/journal.pbio.0040368where" rel="nofollow">http://biology.plosjournals.org/perlserv/?request=slideshow&amp;type=figure&amp;doi=10.1371/journal.pbio.0040368where<sup><img class="rendericon" src="/wiki/images/icons/linkext7.gif" height="7" width="7" align="absmiddle" alt="" border="0"/></sup></a></span> <font color="black"><b>biology</b></font> <font color="black">is the journal  name</font>, <font color="black"><b>figure</b></font> <font color="black">is the image  type</font>, <font color="black">and</font> <font color="black"><b>10.1371/journal.pbio.0040368</b></font> <font color="black">is the  article DOI.</font><br clear="all" /></p>

<p><font color="black">2) Large format JPG images of figures and  tables can be linked to using information including</font></p>
<ul>
	<li><font color="black">journal  name (biology</font>, <font color="black">medicine</font>, <font color="black">compbio</font>, <font color="black">genetics</font>, <font color="black">pathogens</font>, <font color="black">clinicaltrials)</font></li>
	<li><font color="black">journal  eISSN</font></li>
	<li><font color="black">journal  volume</font></li>
	<li><font color="black">journal  issue</font></li>
	<li><font color="black">image  DOI</font></li>
</ul>


<p><font color="black">For example</font>,<br/>
<span class="nobr"><a href="http://biology.plosjournals.org/archive/1545-7885/4/11/figure/10.1371_journal.pbio.0040368.g002-L.jpg" rel="nofollow">http://biology.plosjournals.org/archive/1545-7885/4/11/figure/10.1371_journal.pbio.0040368.g002-L.jpg<sup><img class="rendericon" src="/wiki/images/icons/linkext7.gif" height="7" width="7" align="absmiddle" alt="" border="0"/></sup></a></span><br/>
<font color="black">&nbsp;where</font> <font color="black"><b>biology</b></font> <font color="black">is the journal  name</font>, <font color="black"><b>1545-7885</b></font> <font color="black">is</font> <font color="black"><em>PLoS Biology's</em></font> <font color="black">eISSN</font>, <font color="black">the article is&nbsp;in volume</font> <font color="black"><b>4</b></font>, <font color="black">issue</font> <font color="black"><b>11</b></font>, <font color="black">and the figure DOI is</font> <font color="black"><b>10.1371/journal.pbio.0040368.g002</b></font> <font color="black">(we  substitute an underscore for the forward slash and add a dash L extension  indicating the large format JPG version of the image).</font></p>

<p><font color="black">3) Hi-resolution TIF format images of  figures and tables can be linked to using the image DOI</font>, <font color="black">e.g.</font>,<br/>
http://dx.doi.org/10.1371/journal.pbio.0040368.g002\\</p>

<h3><a name="FormattingContentforProjectorPaperPages-ViewingyourHTMLusingsamestylesthatwillbeusedintheapplication"></a>Viewing your HTML using same styles that will be used in the application</h3>

<p>&nbsp;Attached is an <span class="nobr"><a href="/wiki/download/attachments/7978/gosdb.html?version=1">gosdb.html<sup><img class="rendericon" src="/wiki/images/icons/link_attachment_7.gif" height="7" width="7" align="absmiddle" alt="" border="0"/></sup></a></span> file and a <span class="nobr"><a href="/wiki/download/attachments/7978/jacs.css?version=1">jacs.css<sup><img class="rendericon" src="/wiki/images/icons/link_attachment_7.gif" height="7" width="7" align="absmiddle" alt="" border="0"/></sup></a></span>file.&nbsp; Save them to the same directory and view the html file - It's&nbsp;the&nbsp;actual html used for the gos project page, taken right from the database.</p>

<p>Note</p>

<p>The "&lt;link...&gt;" line at the top has been added so that when you look at the html file in your browser, the styles will look just like in our application.&nbsp; Make sure you remove that line before you&nbsp;add your html to the database.&nbsp;The html is saved to the project.description field of the database.</p>

</body>
</html>