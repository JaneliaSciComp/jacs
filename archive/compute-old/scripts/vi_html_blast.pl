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
getopts("d:f:",\%options);

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
    '_'  => '%5F' ,
    '\`' => '%60' ,
    '{'  => '%7B' ,
    '|'  => '%7C'   );

my $file;
if (defined $options{f}) {
    $file = $options{f};
} else {
    die "Usage: -f <blast filename> [-d <database string>] \n";
}
my $databaseString;
if (defined $options{d}) {
    $databaseString = $options{d};
}

# Create image and map, reformatting link names as necessary
&createImageAndMap($file);
my $mapString;
my $mapName;
open(MAP,"<$file\.map") or die "Could not open $file\.map to read\n";
while(<MAP>) {
    if (/map name=\"(\S+)\"/) {
        $mapName=$1;
    }
    my $mapLine = $_;
    if (/(.+href=\")([^\s]+)(\".*\s)/) {
        my $p1=$1;
        my $label=$2;
        my $p2=$3;
        $mapString .= $p1 . &formatLink($label) . $p2;
    } else {
        $mapString .= $mapLine;
    }
}
close(MAP);
if (! defined $mapName) { die "Map name not defined\n"; }

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
my $imageUrl=&imageUrlFromFile($file);

my $STATE_HEADER=0;
my $STATE_SUMMARY=1;
my $STATE_LABEL=2;
my $STATE_HSP=3;
my $STATE_FOOTER=4;

my $STATE=$STATE_HEADER;

open(HTML, ">$htmlFile") or die "Could not open $htmlFile to write\n";
open(TXT, "<$file") or die "Could not open file $file to read\n";
print HTML "<html>\n<head>\n<meta http-equiv=\"content-type\" content=\"text/html;charset=utf-8\" />\n</head>\n<body>\n";
print HTML "<pre>";
while(<TXT>) {
    if ($STATE==$STATE_HEADER) {
        if (/^(\s*Database:)/) {
            my $dbPrefix=$1;
            if (defined $databaseString) {
                print HTML "$dbPrefix $databaseString\n";
            } else {
                print HTML $_;
            }
        } else {
            print HTML $_;
        }
        if (/^Searching/) {
            $STATE=$STATE_SUMMARY;
            print HTML "</pre>\n";
            print HTML "<p>$mapString\n<img src=\"$imageUrl\" usemap=\"#$mapName\" border=\"0\"/>\n";
            print HTML "<pre>\n";
        }
    } elsif ($STATE==$STATE_SUMMARY) {
        if (/^>\s*([^\s]+)(\s+.*\s)/) {
            my $label=$1;
            my $restOfLine=$2;
            my $pageLink=&formatLink($label);
            $STATE=$STATE_LABEL;
            print HTML "><a name=\"$pageLink\">$label</a>$restOfLine";
        } elsif (/([^\s]+)(\s+.+\s+)(\d+)(\s+[\d\.e-]+\s*)/) {
            my $label=$1;
            my $gap=$2;
            my $score=$3;
            my $paddedEvalue=$4;
            my $pageLink=&formatLink($label);
            print HTML "$label$gap<a href=\"#$pageLink\">$score</a>$paddedEvalue";
        } else {
            print HTML $_;
        }
    } elsif($STATE==$STATE_LABEL) {
        if (!/(\S+)/) { # if line with only whitespace
            $STATE=$STATE_HSP;
        }
        print HTML $_;
    } elsif ($STATE==$STATE_HSP) {
        if (/^>\s*([^\s]+)(\s+.*\s)/) {
            my $label=$1;
            my $restOfLine=$2;
            my $pageLink=&formatLink($label);
            $STATE=$STATE_LABEL;
            print HTML "><a name=\"$pageLink\">$label</a>$restOfLine";
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
print HTML "</body>\n";
print HTML "</html>\n";
close HTML;
close TXT;

###########################################################################################################3

sub createImageAndMap() {
    my $file = $_[0];
    my $searchio = Bio::SearchIO->new(-file => $file, -format => 'blast') or die "parse failed";
    my $result = $searchio->next_result() or die "no result";
    my $panel = Bio::Graphics::Panel->new(-length    => $result->query_length,
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
    my $track = $panel->add_track(-glyph       => 'graded_segments',
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
    while( my $hit = $result->next_hit ) {
        my $feature = Bio::SeqFeature::Generic->new(-score   => $hit->raw_score,
    -display_name => $hit->name,
    -tag     => { description => $hit->description }, );
        while( my $hsp = $hit->next_hsp ) {
            $feature->add_sub_SeqFeature($hsp,'EXPAND');
        }
        $track->add_feature($feature);
    }
    my ($url,$map,$mapname) = $panel->image_and_map(-root => '/tmp',-url => '/tmpimages', -link=>'#$name',);
    my $tmpFile = "/tmp" . $url;
    `mv $tmpFile $file\.png`;
    $?==0 or die "Error moving file";
    open(MAP, ">$file\.map") || die "Could not open file $file\.map to write\n";
    print MAP $map;
    close MAP;
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

sub imageUrlFromFile {
    my $file=$_[0];
    # This expects a filename similar to /data/blast/blast_workspace/1154357211563/blast.out.png
    my @pathUnits=split("/",$file);
    my $orderID = $pathUnits[@pathUnits-2];
    my $url="/blast/ImageServer?OrderID=$orderID";
    return $url;
}

