#!/usr/local/perl

#
# Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
# 
# This file is part of JCVI VICS.
# 
# JCVI VICS is free software; you can redistribute it and/or modify it 
# under the terms and conditions of the Artistic License 2.0.  For 
# details, see the full text of the license in the file LICENSE.txt.  
# No other rights are granted.  Any and all third party software rights 
# to remain with the original developer.
# 
# JCVI VICS is distributed in the hope that it will be useful in 
# bioinformatics applications, but it is provided "AS IS" and WITHOUT 
# ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to implied 
# warranties of merchantability or fitness for any particular purpose.  
# For details, see the full text of the license in the file LICENSE.txt.
# 
# You should have received a copy of the Artistic License 2.0 along with 
# JCVI VICS.  If not, the license can be obtained from 
# "http://www.perlfoundation.org/artistic_license_2_0."
# 

use strict;
use Bio::Graphics;
use Bio::SearchIO;
use Getopt::Std;

my %options=();
getopts("d:f:i:g",\%options);

my $readLinkURL="\/jacs\/detailPage.htm";
my $readLinkNodeParameterKey="nodeId";
my $readLinkEntityParameterKey="acc";

my %urlMap = (
    ' '  => '+' ,
    '!'  => '%21' ,
    '\"' => '%22' ,
    '#'  => '%23' ,
    '\$' => '%24' ,
    '\%' => '%25' ,
    '\&' => '%26' ,
    '\'' => '%27' ,
    '('  => '%28' ,
    ')'  => '%29' ,
    '*'  => '%2A' ,
    '+'  => '%2B' ,
    ','  => '%2C' ,
    '-'  => '%2D' ,
    '.'  => '%2E' ,
    '/'  => '%2F' ,
    ':'  => '%3A' ,
    ';'  => '%3B' ,
    '<'  => '%3C' ,
    '='  => '%3D' ,
    '>'  => '%3E' ,
    '?'  => '%3F' ,
    '\@' => '%40' ,
    '['  => '%5B' ,
    '\\' => '%5C' ,
    ']'  => '%5D' ,
    '^'  => '%5E' ,
#    '_'  => '%5F' , TEMPORARILY DO NOT ESCAPE UNDERSCORES FOR GWT COMPATIBILITY
    '\`' => '%60' ,
    '{'  => '%7B' ,
    '|'  => '%7C'   );

my $file;
if (defined $options{f}) {
    $file = $options{f};
} else {
    die "Usage: -f <blast filename> [-d <database string>] [-i <image filenode id>] [-g add graphics output]\n";
}
my $blastResultFileNodeId;
if (defined $options{i}) {
    $blastResultFileNodeId=$options{i};
}
my $databaseString;
if (defined $options{d}) {
    $databaseString = $options{d};
}
my $graphicsFlag=0;
if (defined $options{g}) {
    $graphicsFlag=1;
}

# Create image and map, reformatting link names as necessary
my $queryCount = &createImageAndMapAndCsv($file); # query count must match below
my %mapNameHash;
my %mapStringHash;
if ($graphicsFlag) {
    for (my $i=0;$i<$queryCount;$i++) {
        my $mapName;
        my $mapString;
        open(MAP,"<$file\.map\_$i") or die "Could not open $file\.map\_$i to read\n";
        while(<MAP>) {
            if (/map name=\"(\S+)\"/) {
                $mapName=$1;
            }
            my $mapLine = $_;
            if (/(.+href=\")([^\s]+)(\".*\s)/) {
                my $p1=$1;
                my $label=$2;
                my $p2=$3;
                $mapString .= $p1 . &formatLink("$label\_$i") . $p2;
            } else {
                $mapString .= $mapLine;
            }
        }
        close(MAP);
        if (! defined $mapName || $mapName eq "") { die "Map name not defined\n"; }
        $mapStringHash{$i}=$mapString;
        $mapNameHash{$i}=$mapName;
    }
}

# Open blast file and step through it, creating an html file which has two kinds of links:
#    1 - From the top-summary to the hit detail
#    2 - From the corresponding position in the html map/image to the hit detail
#
# The approach will be to do this in a single-pass as follows:
#    1 - Create header and appropriate html information
#    2 - Add-in the map and image to the html file, using the label-links
#    3 - For each entry in the summary section, add label-links
#    4 - For each hit section, add link target info

my $htmlFile = $file . "\.html";

my $STATE_HEADER=0;
my $STATE_SUMMARY=1;
my $STATE_LABEL=2;
my $STATE_HSP=3;
my $STATE_FOOTER=4;

my $STATE=$STATE_HEADER;

my $queryCheck=-1;

open(HTML, ">$htmlFile") or die "Could not open $htmlFile to write\n";
open(TXT, "<$file") or die "Could not open file $file to read\n";

#     Removed to generate html snippet instead of document...
#print HTML "<html>\n<head>\n<meta http-equiv=\"content-type\" content=\"text/html;charset=utf-8\" />\n</head>\n<body>\n";
#

print HTML "<script>\n";
print HTML "function setAllCheckBoxes\(formObj, checkValue\)\n";
print HTML "\{\n";
print HTML "     var objCheckBoxes = formObj.elements\[\"id\"\];\n";
print HTML "     if\(!objCheckBoxes\)\n";
print HTML "          return;\n";
print HTML "     var countCheckBoxes = objCheckBoxes.length;\n";
print HTML "     if\(!countCheckBoxes\)\n";
print HTML "          objCheckBoxes.checked = checkValue;\n";
print HTML "     else\n";
print HTML "          \/\/ set the check value for all check boxes\n";
print HTML "          for\(var i = 0; i < countCheckBoxes; i++\)\n";
print HTML "                      objCheckBoxes\[i\]\.checked = checkValue;\n";
print HTML "\}\n";
print HTML "\n";
print HTML "function checkForm \(formObj\)\n";
print HTML "\{\n";
print HTML "    var objCheckBoxes = formObj.elements\[\"id\"\];\n";
print HTML "    if\(!objCheckBoxes\)\n";
print HTML "         return false;\n";
print HTML "    var countCheckBoxes = objCheckBoxes.length;\n";
print HTML "    if\(!countCheckBoxes\)\n";
print HTML "    \{\n";
print HTML "          if \( objCheckBoxes.checked == true\)\n";
print HTML "              return true;\n";
print HTML "    \}\n";
print HTML "    else\n";
print HTML "    \{\n";
print HTML "          \/\/ set the check value for all check boxes\n";
print HTML "          for\(var i = 0; i < countCheckBoxes; i++\)\n";
print HTML "              if \( objCheckBoxes\[i\]\.checked == true\)\n";
print HTML "                  return true;\n";
print HTML "    \}\n";
print HTML "\n";
print HTML "    alert\(\"Please select sequences to export\"\);\n";
print HTML "    return false;\n";
print HTML " \}\n";
print HTML "<\/script>\n";

print HTML "Export all results to csv <a href=\"/jacs/filenode.htm?fn=$blastResultFileNodeId\&tag=rmetacsv\"/>here</a>.<br>\n";
print HTML "<pre>\n</pre>\n";

print HTML "<pre>";
while(<TXT>) {
    if (/BLAST.+\d+\S\d+\S\d+/) { # always watch for start of new query section
        $STATE=$STATE_HEADER;
        if ($queryCheck>=0) { # skip the first one
            print HTML "<hr/>\n";
        }
        print HTML $_;
    } elsif ($STATE==$STATE_HEADER) {
        if (/^Query=/) {
            $queryCheck+=1;
            $STATE=$STATE_SUMMARY;
            if ($graphicsFlag) {
                print HTML "</pre>\n";
                print HTML "<p>$mapStringHash{$queryCheck}\n";
                if (defined $blastResultFileNodeId) {
                    print HTML "<img src=\"/jacs/filenode.htm?fn=$blastResultFileNodeId\&tag=image\&index=$queryCheck\" usemap=\"#$mapNameHash{$queryCheck}\" border=\"0\"/>\n";
                }
                print HTML "<pre>\n";
            } else {
                print HTML $_;
            }
        } else {
            print HTML $_;
        }
    } elsif ($STATE==$STATE_SUMMARY) {
        if (/^(\s*Database:)/) {
            my $dbPrefix=$1;
            if (defined $databaseString) {
                print HTML "$dbPrefix $databaseString\n";
            } else {
                print HTML $_;
            }
        } elsif (/^Searching...done/) {
            print HTML $_ . "</pre><br>";
            print HTML "<form name=\"exportForm\_$queryCheck\" action=\"\/jacs\/fastadelivery.htm\" method=\"post\" onSubmit=\"return checkForm\(this\);\">\n";
            print HTML "<input type=\"hidden\" name=\"suggestedfilename\" value=\"sequences.fasta\" >\n";
            print HTML "<input type=\"button\" onclick=\"setAllCheckBoxes\(this.form, true\)\" value=\"select all\">  ";
            print HTML "<input type=\"button\" onclick=\"setAllCheckBoxes\(this.form, false\)\" value=\"clear all\">  ";
            print HTML "<input type=\"submit\" value=\"Export Sequences\">\n";
            print HTML "<pre>";
        } elsif (/^>\s*([^\s]+)(.*)/) {
            # end of summary when there are hits
            print HTML "</pre>\n";
            print HTML "</form>\n";
            print HTML "<pre>\n";
            my $label=$1;
            my $restOfLine=$2;
            my $pageLink=&formatLink("$label\_$queryCheck");
            print HTML "><a name=\"$pageLink\" href=\"$readLinkURL\?$readLinkEntityParameterKey=$label\">$label</a>\n$restOfLine";
#            print HTML "><a name=\"$pageLink\" href=\"javascript:alert('Not yet implemented')\">$label</a>\n$restOfLine";
            $STATE=$STATE_LABEL;
        } elsif (/([^\s]+)(\s+.+\s+)(\d+)(\s+[\d\.e-]+\s*)/) {
            my $label=$1;
            my $gap=$2;
            my $score=$3;
            my $paddedEvalue=$4;
            my $pageLink=&formatLink("$label\_$queryCheck");
            print HTML "<input type=\"checkbox\" name=\"id\" value=\"$label\">";
            print HTML "<a href=\"$readLinkURL\?$readLinkEntityParameterKey=$label\">$label</a>$gap<a href=\"#$pageLink\">$score</a>$paddedEvalue";
#            print HTML "<a href=\"javascript:alert('Not yet implemented')\">$label</a>$gap<a href=\"#$pageLink\">$score</a>$paddedEvalue";
        } elsif (/No hits found/) {
            print HTML $_;
            # end of summary when there are no hits - put in HSP state to catch footer if at end
            $STATE=$STATE_HSP;
        } else {
            print HTML $_;
        }
    } elsif($STATE==$STATE_LABEL) {
        if (!/(\S+)/) { # if line with only whitespace
            $STATE=$STATE_HSP;
        } else {
            print HTML $_;
        }
    } elsif ($STATE==$STATE_HSP) {
        if (/^>\s*([^\s]+)(.*)/) {
            my $label=$1;
            my $restOfLine=$2;
            my $pageLink=&formatLink("$label\_$queryCheck");
            print HTML "><a name=\"$pageLink\" href=\"$readLinkURL\?$readLinkEntityParameterKey=$label\">$label</a>\n$restOfLine";
#            print HTML "><a name=\"$pageLink\" href=\"javascript:alert('Not yet implemented')\">$label</a>\n$restOfLine";
            $STATE=$STATE_LABEL;
        } elsif (/^(\s*Database:)/) {
            my $dbPrefix=$1;
            $STATE=$STATE_FOOTER;
            print HTML "<pre>\n";
            if (defined $databaseString) {
                print HTML "$dbPrefix $databaseString\n";
            } else {
                print HTML $_;
            }
        } elsif (/^Query=/) {
            $queryCheck+=1;
            print HTML $_;
            $STATE=$STATE_SUMMARY;
            if ($graphicsFlag) {
                print HTML "</pre>\n";
                print HTML "<p>$mapStringHash{$queryCheck}\n";
                if (defined $blastResultFileNodeId) {
                    print HTML "<img src=\"/jacs/filenode.htm?fn=$blastResultFileNodeId\&tag=image\&index=$queryCheck\" usemap=\"#$mapNameHash{$queryCheck}\" border=\"0\"/>\n";
                }
                print HTML "<pre>\n";
            }
        } else {
            print HTML $_;
        }
    } elsif ($STATE==$STATE_FOOTER) {
        print HTML $_;
    } else {
        print HTML $_;
    }
}
print HTML "</pre>\n";

#  Removed to generate html snippet rather than document
#print HTML "</body>\n";
#print HTML "</html>\n";
#

close HTML;
close TXT;

$queryCheck+=1; # final increment to full-count
if ($queryCount!=$queryCheck) {
    print STDERR "Warning: image count is $queryCount and summary count is $queryCheck: they should be the same.\n";
}

###########################################################################################################3

sub createImageAndMapAndCsv() {
    my $file = $_[0];
    my $csvFile = "$file\.csv";
    my $queryCount=0;
    open(CSV, ">$csvFile") || die "Could not open cvs file $csvFile to write\n";
    # Note: The next line must correspond to the csvArr further below
    print CSV "SubjectName," .
              "SubjectDatabase," .
              "QueryName," .
              "HspRank," .
              "NumHsps," .
              "Score," .
              "Bits," .
              "Evalue," .
              "NumConserved," .
              "NumIdentical," .
              "PercentIdentity," .
              "Gaps," .
              "Frame," .
              "QueryMatch," .
              "QueryStart," .
              "Homology," .
              "SubjectMatch," .
              "SubjectStart," .
              "QueryStrand," .
              "SubjectStrand" .
              "\n";

    my $searchio;
    my $panel;
    my $track;
    my $feature;
    $searchio = Bio::SearchIO->new(-file => $file, -format => 'blast') or die "parse failed";
    while(my $result = $searchio->next_result()) {
        if ($graphicsFlag) {
            $panel = Bio::Graphics::Panel->new(-length    => $result->query_length,
                                               -width     => 800,
                                               -pad_left  => 10,
                                               -pad_right => 10,
                                               );
            my $full_length = Bio::SeqFeature::Generic->new(-start=>1,-end=>$result->query_length, -display_name=>$result->query_name);
            $panel->add_track($full_length,
                              -glyph   => 'arrow',
                              -tick    => 2,
                              -fgcolor => 'black',
                              -double  => 1,
                              -label   => 1,
                              );
            $track = $panel->add_track(-glyph       => 'graded_segments',
                                       -label       => 1,
                                       -connector   => 'dashed',
                                       -bgcolor     => 'blue',
                                       -font2color  => 'red',
                                       -sort_order  => 'high_score',
                                       -description => sub { my $feature = shift;
                                                             return unless $feature->has_tag('description');
                                                             my ($description) = $feature->each_tag_value('description');
                                                             my $score = $feature->score;
                                                             "$description, score=$score";
                                                         }
                                       );
        }
        while( my $hit = $result->next_hit ) {
            if ($graphicsFlag) {
                $feature = Bio::SeqFeature::Generic->new(-score   => $hit->raw_score,
                                                         -display_name => $hit->name,
                                                         -tag     => { description => $hit->description }, );
            }
            my $csvDatabase;
            if (defined $databaseString) {
                $csvDatabase=$databaseString;
            } else {
                $csvDatabase=$result->database_name;
            }
            while( my $hsp = $hit->next_hsp ) {
                if ($graphicsFlag) {
                    $feature->add_sub_SeqFeature($hsp,'EXPAND');
                }
                my $frameString = $hsp->query->frame . " " . $hsp->hit->frame;
                my $queryString = $result->query_name . " " . $result->query_description;
                my @csvArr = ( $queryString,
                               $result->algorithm,
                               $hit->name,
                               $csvDatabase,
                               $hsp->rank,
                               $hit->num_hsps,
                               $hsp->score,
                               $hsp->bits,
                               $hsp->evalue,
                               $hsp->num_conserved,
                               $hsp->num_identical,
                               $hsp->percent_identity,
                               $hsp->gaps,
                               $frameString,
                               $hsp->query_string,
                               $hsp->start('query'),
                               $hsp->homology_string,
                               $hsp->hit_string,
                               $hsp->start('hit'),
                               $hsp->strand('query'),
                               $hsp->strand('hit') );
                my $csvStart=0;
                foreach my $csvEntry (@csvArr) {
                    if (defined $csvEntry) {
                        if ($csvStart!=0) {
                            print CSV ",";
                        }
                        $csvStart+=1;
                        $csvEntry=~s/,/ /g;
                        print CSV $csvEntry;
                    }
                }
                print CSV "\n";
                if ($csvStart!=21) {
                    die "Found $csvStart entries rather than 21 in array @csvArr\n";
                }
            }
            if ($graphicsFlag) {
                $track->add_feature($feature);
            }
        }
        if ($graphicsFlag) {
            my ($url,$map,$mapname) = $panel->image_and_map(-root => '/tmp',-url => '/tmpimages', -link=>'#$name',);
            my $tmpFile = "/tmp" . $url;
            `mv $tmpFile $file\.png\_$queryCount`;
            $?==0 or die "Error moving file";
            open(MAP, ">$file\.map\_$queryCount") || die "Could not open file $file\.map\_$queryCount to write\n";
            print MAP $map;
            close MAP;
        }
        $queryCount+=1;
    }
    close CSV;
    return $queryCount;
}

sub formatLink {
    my $linkToFormat = $_[0];
    my @charArr=split(//,$linkToFormat);
    my $resultLink="";
    foreach my $ch (@charArr) {
        if ($resultLink eq "" && $ch eq '#') {
            # skip
            $resultLink .= $ch;
        } elsif (defined $urlMap{$ch}) {
            $resultLink .= $urlMap{$ch};
        } else {
            $resultLink .= $ch;
        }
    }
    return $resultLink;
}
