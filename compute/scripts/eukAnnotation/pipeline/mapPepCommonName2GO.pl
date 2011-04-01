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
our $errorMessage;
our $loThreshold = 0;
our $hiThreshold = 7.5;

require "getopts.pl";
use Cwd 'realpath';
use File::Basename;

my $program = realpath($0);
my $myLib = dirname($program);
use lib ('/usr/local/devel/ANNOTATION/EAP/pipeline');
use EAP::generic;

# read GO terms
my $goFile = $ARGV[0];

my %goTerms;
my %goKeys;
open( GOFILE, "<$goFile" );
while ( my $line = <GOFILE> ) {
	if ( substr( $line, 0, 3 ) ne "GO:" ) { next }
	chomp $line;
	my ( $goId, $goName, $goCatg ) = split /\t/, $line;
#	print "$goId\t$goName\t$goCatg";
#	if ( uc( $goCatg ) ne uc ( $ARGV[1] ) ) {
#		print "..skipped\n";
#		next;
#	} else {
#		print "\n";
#	}
	
# normalize go name
# lowercase, all punctuation changed to blank, leading/trailing/consecutive blanks removed
	$goTerms{$goId}{name} = $goName;
	$goTerms{$goId}{category} = $goCatg;
	$goName = normalizeName ( $goName );
	if ( length( $goName ) < 5 ) { next }
	$goTerms{$goId}{normalized} = $goName;
#	print "$goId\t\"$goName\"\n";

# break go name into keywords at blanks
	my $keywords = parseKeywords( $goName );
	$goTerms{$goId}{keywords} = $keywords;
	$goTerms{$goId}{maxscore} = 0;
	for my $keyword ( keys %$keywords ) {
		$goKeys{$keyword}{goIds}{$goId}++;
		$goTerms{$goId}{maxscore} += length( $keyword );
		my $alternates = $$keywords{$keyword}{alternates};
		if ( defined $alternates ) {
			for my $keystr ( keys %$alternates ) {
				$goKeys{$keystr}{goIds}{$goId}++;
			}
		}
	}
}
close GOFILE;

# read protein common names
my $himatchfile = $ARGV[1] . ".goHi";
my $lomatchfile = $ARGV[1] . ".goLo";
my $nomatchfile = $ARGV[1] . ".goNo";
open( HIMATCH, ">$himatchfile" );
open( LOMATCH, ">$lomatchfile" );
open( NOMATCH, ">$nomatchfile" );
open( COMNAME, "<$ARGV[0]" );
{
	my $line = <COMNAME>;
}
while ( my $line = <COMNAME> ) {
	chomp $line;
	my ( $proteinId, $commonName );
	{
		my @tmp = split /\t/, $line;
		$proteinId = $tmp[0];
		$commonName = $tmp[1];
	}
	my $normalized = normalizeName( $commonName );
	if ( length( $normalized ) == 0 ) { next }

# break common name into keywords at blanks
# save each keyword
# for keywords >= 5 characters long, save alternate spellings
# (produced by dropping each letter, one-at-a-time) to catch typos, plurals, etc.
	my $keywords = parseKeywords( $normalized );
	my %goList;
	for my $keyword ( keys %$keywords ) {

# use keys to find possible go matches
		my $goIds = $goKeys{$keyword}{goIds};
		foreach my $goId ( keys %$goIds ) {
			if ( !exists $goList{$goId} ) {	$goList{$goId} = $goTerms{$goId} }
		}
		my $alternates = $$keywords{alternates};
		if ( defined $alternates ) {
			foreach my $keystr ( keys %$alternates ) {
				$goIds = $goKeys{$keystr}{goIds};
				foreach my $goId ( keys %$goIds ) {
					if ( !exists $goList{$goId} ) {	$goList{$goId} = $goTerms{$goId} }
				}
			}
		}
	}

# score possible matches
	my @matches = ();
	my @possibleMatches = ();
	for my $goId ( keys %goList ) {
		my $goscore = scoreMatch( $normalized, $goList{$goId}{normalized} );
		if ( $goscore >= $loThreshold ) {
			my %match;
			$match{goId} = $goId;
			$match{matchScore} = $goscore;
			if ( $goscore >= $hiThreshold ) {
				$match{quality} = 1;
			} else {
				$match{quality} = 0;
			}
			push @matches, \%match;
		}
	}

	if ( @matches ) {
		@matches = removeRedundantMatches( \@matches );
		@matches = reverse sort { $$a{matchScore} <=> $$b{matchScore} } @matches;

		for my $match( @matches ) {
			print join( "\t", ( $proteinId, $commonName, $$match{matchScore}, $$match{goId}, $goTerms{$$match{goId}}{name} ) ) . "\n";
			if ( $$match{quality} ) {
				print HIMATCH join( "\t", ( $proteinId, $commonName, $$match{matchScore}, $$match{goId}, $goTerms{$$match{goId}}{name} ) ) . "\n";
			} else {
				print LOMATCH join( "\t", ( $proteinId, $commonName, $$match{matchScore}, $$match{goId}, $goTerms{$$match{goId}}{name} ) ) . "\n";
			}
		}		

	} else {
		print join( "\t", ( $proteinId, $commonName ) ) . "\n";		
		print NOMATCH join( "\t", ( $proteinId, $commonName ) ) . "\n";
	}
}

close( HIMATCH );
close( LOMATCH );
close( NOMATCH );
exit(0);

sub removeRedundantMatches {
	my ( $data ) = @_;

	my @matches = sort { length($goTerms{$$a{goId}}{normalized}) <=> length($goTerms{$$b{goId}}{normalized}) } @$data;
	my $lastMatch = scalar @matches - 1;
	for my $i ( 0 .. $lastMatch ) {
		if ( ! defined $matches[$i] ) { next }
		my $matchI = $matches[$i];
		for my $j ( $i+1 .. $lastMatch ) {
			if ( ! defined $matches[$j] ) { next };
			my $matchJ = $matches[$j]; 
			if ( $$matchJ{quality} < $$matchI{quality} ) { next }
			if ( $goTerms{$$matchI{goId}}{category} eq $goTerms{$$matchJ{goId}}{category} ) {
				my $goscore = scoreMatch( $goTerms{$$matchJ{goId}}{normalized}, $goTerms{$$matchI{goId}}{normalized} );
				if ( $goscore >= $hiThreshold ) {
print "   $goTerms{$$matchJ{goId}}{normalized} ($$matchJ{quality}) supersedes $goTerms{$$matchI{goId}}{normalized} ($$matchI{quality})\n";					$matches[$i] = undef;
				}
			}
		}
	}
	my @tmp;
	for my $i ( 0 .. $lastMatch ) {
		if ( defined $matches[$i] ) {
			push @tmp, $matches[$i];
		}
	}
	return @tmp;
}

sub scoreMatch{
	my ( $common_name, $go_name ) = @_;
#print "====================================================================\n";
#print "searching for \"$go_name\" in \"$common_name\"\n";

	my @gokeys = split( " ", $go_name );
	my $gpos = 0;
	my $glast = scalar @gokeys - 1;
	my $maxscore = 10 * @gokeys;

	my @commonkeys = split( " ", $common_name );
	my $cpos = 0;
	my $clast = scalar @commonkeys - 1;
	my $score = 0;
	my $orderptr = -1;

# exact match, forwards
	while ( $gpos <= $glast ) {
		my $wordscore;
		my $gokeylen = length( $gokeys[$gpos] );
		my $cpos = $orderptr;
		my $penalty = 0;

		while ( $cpos < $clast && !defined $wordscore ) {
			$cpos++;

			if ( !defined $commonkeys[$cpos] ) {

			} elsif ( $commonkeys[$cpos] eq $gokeys[$gpos] ) {
				$wordscore = 10 - $penalty;
				$commonkeys[$cpos] = undef;	#prevent re-user

			} else {
				if ( $orderptr >= 0 && $orderptr <= $clast ) {
					if ( $commonkeys[$cpos] =~ /porter/ ||
							$commonkeys[$cpos] =~ /binding/ ||
							$commonkeys[$cpos] =~ /helicase/ ||
							$commonkeys[$cpos] =~ /assembly/ ||
							$commonkeys[$cpos] =~ /protein/ ||
							$commonkeys[$cpos] =~ /dna/ ||
							$commonkeys[$cpos] =~ /rna/ ||
							$commonkeys[$cpos] =~ /transport/ ) {
						$penalty = $penalty + 2;
					} else {
						$penalty = $penalty + 0.75;
					}
				}
			}
		}

# suffix/prefix match, forwards
		if ( !defined $wordscore && $gokeylen >= 5 ) {
			$cpos = $orderptr;
			$penalty = 0;

			while ( $cpos < $clast && !defined $wordscore ) {
				$cpos++;

				if ( !defined $commonkeys[$cpos] ) {

				} elsif ( $gokeys[$gpos]."s" eq $commonkeys[$cpos]
							|| $gokeys[$gpos]."es" eq $commonkeys[$cpos]
							|| $gokeys[$gpos]."ed" eq $commonkeys[$cpos]
							|| $gokeys[$gpos]."ing" eq $commonkeys[$cpos]
							|| $gokeys[$gpos] eq $commonkeys[$cpos]."s"
							|| $gokeys[$gpos] eq $commonkeys[$cpos]."es"
							|| $gokeys[$gpos] eq $commonkeys[$cpos]."ed"
							|| $gokeys[$gpos] eq $commonkeys[$cpos]."ing" ) {
					$wordscore = 10 - $penalty;
					$commonkeys[$cpos] = undef;	#prevent re-user

				} elsif ( $gokeys[$gpos] eq substr( $commonkeys[$cpos], 0, $gokeylen ) ||
							$gokeys[$gpos] eq substr( $commonkeys[$cpos], length( $commonkeys[$cpos] ) - $gokeylen) ) {
					$wordscore = 8.5 - $penalty;
					$commonkeys[$cpos] = undef;	#prevent re-user

				} else {
					if ( $orderptr >= 0 && $orderptr <= $clast ) {
						if ( $commonkeys[$cpos] =~ /porter/ ||
								$commonkeys[$cpos] =~ /binding/ ||
								$commonkeys[$cpos] =~ /helicase/ ||
								$commonkeys[$cpos] =~ /assembly/ ||
								$commonkeys[$cpos] =~ /protein/ ||
								$commonkeys[$cpos] =~ /dna/ ||
								$commonkeys[$cpos] =~ /rna/ ||
								$commonkeys[$cpos] =~ /transport/ ) {
							$penalty = $penalty + 2;
						} else {
							$penalty = $penalty + 0.75;
						}
					}
				}
			}
		}

# exact match, backwards
		if ( !defined $wordscore ) {
			$cpos = $orderptr;
			my $penalty = 0.75;
			if ( $gokeys[$gpos] =~ /porter/ ||
					$gokeys[$gpos] =~ /binding/ ||
					$gokeys[$gpos] =~ /helicase/ ||
					$gokeys[$gpos] =~ /assembly/ ||
					$gokeys[$gpos] =~ /transport/ ) {
				$penalty = 2;
			}

			while ( $cpos > 0 && !defined $wordscore ) {
				$cpos--;

				if ( !defined $commonkeys[$cpos] ) {

				} elsif ( $commonkeys[$cpos] eq $gokeys[$gpos] ) {
					$wordscore = 10 - $penalty;
					$commonkeys[$cpos] = undef;	#prevent re-user

				} else {
					if ( $orderptr >= 0 && $orderptr <= $clast ) {
						if ( $gokeys[$gpos] =~ /porter/ ||
								$gokeys[$gpos] =~ /binding/ ||
								$gokeys[$gpos] =~ /helicase/ ||
								$gokeys[$gpos] =~ /assembly/ ||
								$gokeys[$gpos] =~ /transport/ ) {
							$penalty = $penalty + 2;
						} else {
							$penalty = $penalty + 0.75;
						}
					}
				}
			}
		}

# suffix/prefix match, backwards
		if ( !defined $wordscore && $gokeylen >= 5 ) {
			$cpos = $orderptr;
			my $penalty = 0.75;
			if ( $gokeys[$gpos] =~ /porter/ ||
					$gokeys[$gpos] =~ /binding/ ||
					$gokeys[$gpos] =~ /helicase/ ||
					$gokeys[$gpos] =~ /assembly/ ||
					$gokeys[$gpos] =~ /transport/ ) {
				$penalty = 2;
			}

			while ( $cpos > 0 && !defined $wordscore ) {
				$cpos--;

				if ( !defined $commonkeys[$cpos] ) {

				} elsif ( $gokeys[$gpos]."s" eq $commonkeys[$cpos]
							|| $gokeys[$gpos]."es" eq $commonkeys[$cpos]
							|| $gokeys[$gpos]."ed" eq $commonkeys[$cpos]
							|| $gokeys[$gpos]."ing" eq $commonkeys[$cpos]
							|| $gokeys[$gpos] eq $commonkeys[$cpos]."s"
							|| $gokeys[$gpos] eq $commonkeys[$cpos]."es"
							|| $gokeys[$gpos] eq $commonkeys[$cpos]."ed"
							|| $gokeys[$gpos] eq $commonkeys[$cpos]."ing" ) {
					$wordscore = 10 - $penalty;
					$commonkeys[$cpos] = undef;	#prevent re-user

				} elsif ( $gokeys[$gpos] eq substr( $commonkeys[$cpos], 0, $gokeylen ) ||
							$gokeys[$gpos] eq substr( $commonkeys[$cpos], length( $commonkeys[$cpos] ) - $gokeylen) ) {
					$wordscore = 8.5 - $penalty;
					$commonkeys[$cpos] = undef;	#prevent re-user

				} else {
					if ( $orderptr >= 0 && $orderptr <= $clast ) {
						if ( $gokeys[$gpos] =~ /porter/ ||
								$gokeys[$gpos] =~ /binding/ ||
								$gokeys[$gpos] =~ /helicase/ ||
								$gokeys[$gpos] =~ /assembly/ ||
								$gokeys[$gpos] =~ /protein/ ||
								$gokeys[$gpos] =~ /dna/ ||
								$gokeys[$gpos] =~ /rna/ ||
								$gokeys[$gpos] =~ /transport/ ) {
							$penalty = $penalty + 2;
						} else {
							$penalty = $penalty + 0.75;
						}
					}
				}
			}
		}

# update score and pointers
		if ( defined $wordscore ) {
			$score += $wordscore;
			$orderptr = $cpos;

# "ion" is often omitted, lessen the penalty
		} elsif ( $gokeys[$gpos] eq "\"ion\"" ) {
			$maxscore = $maxscore - 5;
		}
		$gpos++;
	}
#	print "score is $score out of $maxscore\n";
#	print "====================================================================\n";
	
# return results
	$score = int( 1000. * $score / $maxscore + 0.5 ) / 10. - 90.;
	return $score;
}

# search for presence of keyword in string
# allows for exact match, spelling error, and prefixed, embedded, and suffixed matches
sub scoreKeyword {
	my ( $keyword, $alternates, $candidateString ) = @_;
	my $searchString = " $candidateString ";
	
# exact keyword match
	if ( index( $searchString, " $keyword " ) >= 0 ) {
		return length( $keyword );
	}

# look for inexact matches
	if ( length( $keyword ) <= 5 ) { return -1 }

# alternate spelling matches keyword
	if ( defined $alternates ) {
		for my $keystr ( keys %$alternates ) {
			if ( index( $searchString, " $keystr " ) >= 0 ) {
#print "alternate spelling: \"$keystr\"\n";
				return length( $keyword ) - 1;
			}
		}
	}
# keyword matches start of keyword
	if ( index( $searchString, " $keyword" ) >= 0 ) {
#print "prefixed: \"$keyword%\"\n";
		return length( $keyword ) - 1;

# alternate spelling matches start of keyword
	} elsif ( defined $alternates ) {
		for my $keystr ( keys %$alternates ) {
			if ( index( $searchString, " $keystr" ) >= 0 ) {
#print "alternate prefixed: \"$keystr%\"\n";
				return length( $keyword ) - 2;
			}
		}
	}

# keyword matches end of keyword
	if ( index( $searchString, "$keyword " ) >= 0 ) {
#print "suffixed: \"%$keyword\"\n";
		return length( $keyword ) - 1;

# alternate spelling matches end of keyword
	} elsif ( defined $alternates ) {
		for my $keystr ( keys %$alternates ) {
			if ( index( $searchString, "$keystr " ) >= 0 ) {
#print "alternate suffixed: \"%$keystr\"\n";
				return length( $keyword ) - 2.;
			}
		}
	}

# keyword embedded in keyword
	if ( index( $searchString, $keyword ) >= 0 ) {
#print "embedded: \"%$keyword%\"\n";
		return length( $keyword ) - 2.;

# alternate spelling embedded in keyword
	} elsif ( defined $alternates ) {
		for my $keystr ( keys %$alternates ) {
			if ( index( $searchString, $keystr ) >= 0 ) {
#print "alternate embedded: \"%$keystr%\"\n";
				return length( $keyword ) - 3.;
			}
		}
	}

# no match
	return -1;
}

# normalize string
# convert text to lowercase
# remove some strings that interfere with name comparisons
# convert all punctuation to blanks and remove leading, trailing, and consecutive blanks
sub normalizeName {
	my ( $name ) = @_;
	
# add leading/trailing blank to simplify parsing
	my $normalized = " $name ";
	
## turn ""a/b/c verb" to "a verb/b verb/c verb"
#	if ( index( $name, "/") >= 0 ) {
#		my @tmp = split( " ", $name );
#		my $newname;
#		my $last = scalar @tmp - 1;
#		my $prev = -1;
#		for my $i ( 0 .. $last - 1 ) {
#			if ( index( "$tmp[$i]", "/" ) >= 0 ) {
#				if ( $tmp[$i+1] =~ /binding/i ||
#						$tmp[$i+1] =~ /transport/i ||
#						$tmp[$i+1] =~ /antiport/i ||
#						$tmp[$i+1] =~ /symport/i	||
#						$tmp[$i+1] =~ /receptor/i ||
#						$tmp[$i+1] =~ /signal/i ||
#						$tmp[$i+1] =~ /helicase/i ) {
#					if ( $prev+1 <= $i-1 ) {
#						$newname .= " " . join( " ", @tmp[$prev+1..$i-1] );
#					}
#					for my $slash ( split( "/", $tmp[$i] ) ) {
#						$newname .= " " . $slash . " " . $tmp[$i+1] . " /";
#					}
#					$newname =~ s/ \/$//;
#					$prev = $i + 1;
#				}
#			}
#		}
#		if ( defined $newname && $prev+1 <= $last ) {
#			$newname .= " " . join( " ", @tmp[$prev+1..$last] );
#		}
#		if ( defined $newname ) {
#			print "changed $name to $newname\n";
#			$name = $newname;
#		}
#	}

# convert all punctation to blank
# except for +-( and ) which will recieve special handling in chemical formulae
	$normalized =~ s/[^a-zA-Z0-9"+\-()]/ /g;

# spell out some common chemical formulae
	$normalized =~ s/ NO2- / nitrite /g;
	$normalized =~ s/ NO3- / nitrate /g;
	$normalized =~ s/ NO-2 / nitrite /g;
	$normalized =~ s/ NO-3 / nitrate /g;
	$normalized =~ s/ Fe\+3 / ferric iron ion /g;
	$normalized =~ s/ Fe3\+ / ferric iron ion /g;
	$normalized =~ s/ ferric / ferric iron ion /gi;
	$normalized =~ s/ iron(iii) / ferric iron ion /gi;
	$normalized =~ s/ Fe / iron /g;
	$normalized =~ s/ Fe\+2 / ferrous iron ion /g;
	$normalized =~ s/ Fe2\+ / ferrous iron ion /g;
	$normalized =~ s/ ferrous / ferrous iron ion  /gi;
	$normalized =~ s/ iron(ii) / ferrous iron ion  /gi;
	$normalized =~ s/([0-9]Fe)-([0-9]S[^a-z])/$1$2/g; 
	$normalized =~ s/ Zn / zinc /g;
	$normalized =~ s/ Zn[0-9]\+ / zinc ion /g;
	$normalized =~ s/ Zn\+[0-9] / zinc ion /g;
	$normalized =~ s/ Mn / manganese /g;
	$normalized =~ s/ Mn[0-9]\+ / manganese ion /g;
	$normalized =~ s/ Mn\+[0-9] / manganese ion /g;
	$normalized =~ s/ Hg / mercury /g;
	$normalized =~ s/ Hg[0-9]\+ / mercury ion /g;
	$normalized =~ s/ Hg\+[0-9] / mercury ion /g;
	$normalized =~ s/ Cd / cadmium /g;
	$normalized =~ s/ Cd[0-9]\+ / cadmium ion /g;
	$normalized =~ s/ Cd\+[0-9] / cadmium ion /g;
	$normalized =~ s/ Co / cobalt /;
	$normalized =~ s/ Co[0-9]\+ / cobalt ion /;
	$normalized =~ s/ Co\+[0-9] / cobalt ion /;
	$normalized =~ s/ Ni / nickel /g;
	$normalized =~ s/ No[0-9]\+ / nickel ion /;
	$normalized =~ s/ Ni\+[0-9] / nickel ion /;
	$normalized =~ s/ Cu / copper /g;
	$normalized =~ s/ Cu[0-9]\+ / copper ion /;
	$normalized =~ s/ Cu\+[0-9] / copper ion /;
	$normalized =~ s/ Sn / tin /g;
	$normalized =~ s/ Sn[0-9]\+ / tin ion /;
	$normalized =~ s/ Sn\+[0-9] / tin ion /;

	$normalized =~ s/ iron /  metaliron /g;
	$normalized =~ s/ nickel /  metalnickel /g;
	$normalized =~ s/ copper /  metalcopper/g;
	$normalized =~ s/ tin /  metaltin /g;
	$normalized =~ s/ zinc / metalzinc /g;
	$normalized =~ s/ manganese / metalmanganese /g;
	$normalized =~ s/ mercury /  metalmercury /g;
	$normalized =~ s/ cobalt / metalcobalt /g;
	$normalized =~ s/ cadmium / metalcadmium /g;

	$normalized =~ s/ Pb / metallead /g;
	$normalized =~ s/ Pb[0-9]\+ / metallead ion /;
	$normalized =~ s/ Pb\+[0-9] / metallead ion /;

# handle 1,2 and 3- and (N) and so forth, in chemical names
	$normalized =~ s/[\[\]()"'`]//g;
	$normalized =~ s/-([0-9])-/"$1 /g;
	$normalized =~ s/-([0-9]) /"$1 /g;
	$normalized =~ s/ ([0-9])-/ "$1/g;
	$normalized =~ s/([0-9]),([0-9])/"$1"$2/g;
	$normalized =~ s/\(([A-Z])\)/"$1"/g;
	$normalized =~ s/-([A-Z])-/ $1"/g;

# molydopterin cofactors
	$normalized =~ s/mo-molybdopterin/mo molybdopterin/gi;
	$normalized =~ s/w-molybdopterin/"tungsten w molybdopterin/gi;
	$normalized =~ s/molybdopterin cofactor/molybdopterin/gi;
	$normalized =~ s/molybdenum cofactor/"mo molybdopterin/gi;

# convert remaining punctuation to blanks
# (except for ", added above to indicate special handling)
# and remove duplicate blanks
	$normalized =~ s/[^a-zA-Z0-9"]/ /g;

# remove some common short words
	$normalized =~ s/ and / /;
	$normalized =~ s/ or / /;
	$normalized =~ s/ of / /;
	$normalized =~ s/ from / /;
  	$normalized =~ s/ to / /;
	$normalized =~ s/ by / /;
	$normalized =~ s/ via / /;
	$normalized =~ s/ in / /;
	$normalized =~ s/ on / /;
	$normalized =~ s/ for / /;
	$normalized =~ s/ as / /;
	$normalized =~ s/ at / /;

# convert to lowercase
	$normalized = lc $normalized;
	$normalized =~ s/  */ /g;

# remove some common terms which do not add information
	$normalized =~ s/ process / /g;
	$normalized =~ s/ activity / /g;
	$normalized =~ s/ region / /g;
	$normalized =~ s/ complex / /g;
	$normalized =~ s/ hypothetical / /g;
	$normalized =~ s/ unknown / /g;
	$normalized =~ s/ putative / /g;
	$normalized =~ s/ probable / /g;
	$normalized =~ s/ like / /g;

# common abbreviations
	$normalized =~ s/ abc / atp binding cassette /g;
	$normalized =~ s/ atp binding cassette transport/ transport atp binding cassette transport/;
		
# stemming and synonyms of some important keywords
	$normalized =~ s/ flagellate / flagella /g;
	$normalized =~ s/ flagellation / flagella /g;
	$normalized =~ s/ flagellum / flagella /g;
	$normalized =~ s/ flagellin / flagella /g;
	$normalized =~ s/ flagellar / flagella /g;
	$normalized =~ s/ ribosomes / ribosome /g;
	$normalized =~ s/ ribosomal / ribosome /g;
	$normalized =~ s/ sulph / sulf /g;
	$normalized =~ s/ sulfer / sulfur /g;
	$normalized =~ s/ iron sulfur cluster / iron sulfur /g;
	$normalized =~ s/ iron s cluster / iron sulfur /g;
	$normalized =~ s/ haemagluttinin / hemaglutinin /g;
	$normalized =~ s/ haemaglutinin / hemaglutinin /g;
	$normalized =~ s/ hemagluttinin / hemaglutinin /g;
	$normalized =~ s/ metabolization / metabolic /g;
	$normalized =~ s/ metabolizing / metabolic /g;
	$normalized =~ s/ metabolizes / metabolic /g;
	$normalized =~ s/ metabolites / metabolic /g;
	$normalized =~ s/ metabolize / metabolic /g;
	$normalized =~ s/ metabolite / metabolic /g;
	$normalized =~ s/ metabolism / metabolic /g;
	$normalized =~ s/ mitochondrial / mitochondria /g;
	$normalized =~ s/ transcriptional / transcript /g;
	$normalized =~ s/ transcription / transcript /g;
	$normalized =~ s/ transcribing / transcript /g;
	$normalized =~ s/ transcripts / transcript /g;
	$normalized =~ s/ transcribes / transcript /g;
	$normalized =~ s/ transcribe / transcript /g;
	$normalized =~ s/ translocation / translocate /g;
	$normalized =~ s/ translocating / translocate /g;
	$normalized =~ s/ translocator / translocate /g;
	$normalized =~ s/ translocates / translocate /g;
	$normalized =~ s/ transporting / transport /g;
	$normalized =~ s/ transporter / transport /g;
	$normalized =~ s/ transports / transport /g;
	$normalized =~ s/ regulatory / regulate /g;
	$normalized =~ s/ regulation / regulate /g;
	$normalized =~ s/ regulators / regulate /g;
	$normalized =~ s/ regulating / regulate /g;
	$normalized =~ s/ regulator / regulate /g;
	$normalized =~ s/ regulates / regulate /g;
	$normalized =~ s/ regulon / regulate /g;
	$normalized =~ s/ secretions / secretion /g;
	$normalized =~ s/ secretory / secretion /g;
	$normalized =~ s/ secreted / secretion /g;
	$normalized =~ s/ secretes / secretion /g;
	$normalized =~ s/ secrete / secretion /g;
	$normalized =~ s/ perception / sensory /g;
	$normalized =~ s/ sensation / sensory /g;
	$normalized =~ s/ sensing / sensory /g;
	$normalized =~ s/ senses / sensory /g;
	$normalized =~ s/ sense / sensory /g;
	$normalized =~ s/ signalling / signal /g;
	$normalized =~ s/ signalled / signal /g;
	$normalized =~ s/ signaling / signal /g;
	$normalized =~ s/ signals / signal /g;
	$normalized =~ s/ centromeric / centromere /g;  
	$normalized =~ s/ biosynthetic / synthesis /g;
	$normalized =~ s/ biosynthesis / synthesis /g;
	$normalized =~ s/ anabolic / synthesis /g;
	$normalized =~ s/ catabolism / catabolic /g;
	$normalized =~ s/ chaperonin / chaperone /g; 
	$normalized =~ s/ multidrug / multi drug /g;

# prevent "protein"" from being backwards-associated with common functions
	$normalized =~ s/proteins/protein/g;
	$normalized =~ s/binding protein/binding/g;
	$normalized =~ s/transport protein/transport/;
	$normalized =~ s/chaperone protein/chaperone/;
	$normalized =~ s/channel protein/channel/;

# prevent some prefix/suffix matchings
	$normalized =~ s/ independent / "independent" /g;
	$normalized =~ s/ non ribosomal / "non ribosomal" /g;
	$normalized =~ s/ hydrogen / "hydrogen" /g;
	$normalized =~ s/ galactose / "galactose" /g;
	$normalized =~ s/ ion / "ion" /g;
	$normalized =~ s/ cation / "cat"ion" /g;
	$normalized =~ s/ anion / "an"ion" /g;
	$normalized =~ s/ trans / "trans" /g;
	$normalized =~ s/([a-z0-9]dna)/"$1"/g;
	$normalized =~ s/([a-z0-9]rna)/"$1"/g;
	$normalized =~ s/(dna[a-z0-9])/"$1"/g;
	$normalized =~ s/(rna[a-z0-9])/"$1"/g;

# handle some order important phrasing
#	$normalized =~ s/ protein ([a-z0-9"])/ protein protein$1/;
#	$normalized =~ s/ dna ([a-z0-9"])/ dna dna$1/g;
#	$normalized =~ s/ rna ([a-z0-9"])/ rna rna$1/g;
#	$normalized =~ s/ drug ([a-z0-9"])/ drug drug$1/g;
	$normalized =~ s/ transcript regulate / regulate transcript /g;
 
# remove leading and trailing blanks
	$normalized =~ s/^  *//;
	$normalized =~ s/  *$//;

	if ( $normalized eq "protein" || $normalized eq "gene" ) { $normalized = "" }
#print "IN: \"$name\" OUT: \"$normalized\"\n";	
	return $normalized;
}
# parse string into keywords
# for keywords >= 5 characters long save alternate spellings to catch typos, plurals, etc.
# (produced by dropping each letter, one-at-a-time)
sub parseKeywords {
	my ( $name ) = @_;

	my %keywords;
	for my $keyword ( split / /, $name ) {
#		if ( length( $keyword ) > 5 ) {
#			for my $i ( 1 .. length( $keyword ) - 1 ) {
#				my $keystr;
#				if ( $i == 0 ) {
#					$keystr = substr( $keyword, 1 );
##print "keyword \"$keyword\" alternate \"$keystr\"\n";
#				} elsif ( $i == length( $keyword ) - 1 ) {
#					$keystr = substr( $keyword, 0, $i );
##print "keyword \"$keyword\" alternate \"$keystr\"\n";
#				} else {
#					$keystr = substr( $keyword, 0, $i ) . substr( $keyword, $i+1 );
##print "keyword \"$keyword\" alternate \"$keystr\"\n";
#				}
#				$keywords{$keyword}{alternates}{$keystr}++;
#			}
#		} else {
			$keywords{$keyword}{alternates} = undef;
#print "keyword \"$keyword\" no alternates\n";
#		}	
	}

	return \%keywords;	
}
