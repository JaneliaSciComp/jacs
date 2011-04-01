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
#our $vicsWSDL = "http://camdev1:8080/compute-compute/ComputeWS?wsdl";	#rpsblast patch
#our $vicsWSDL = "http://metagenomics-proda:8180/compute-compute/ComputeWS?wsdl";	#mini-grid
#our $vicsWSDL = "http://saffordt-ws1:8180/compute-compute/ComputeWS?wsdl";	#production
#our $vicsWSDL = "http://camdev6:8080/compute-compute/ComputeWS?wsdl";	#test
our $vicsWSDL = "http://camdev3:8180/compute-compute/ComputeWS?wsdl";	#jason
#our $vicsWSDL = "http://camapp:8080/compute-compute/ComputeWS?wsdl";	#development
#our $vicsWSDL = "http://limsdev1:8080/compute-compute/ComputeWS?wsdl";	#adam
#our $vicsWSDL = "http://camdev4:8080/compute-compute/ComputeWS?wsdl";	#sree

require "getopts.pl";
use Cwd 'realpath';
use File::Basename;

my $program = realpath($0);
our $myLib = dirname($program);
use lib ('/usr/local/devel/ANNOTATION/EAP/pipeline');
use EAP::vics;

my $owner = "system";
my $workspace;
my $name = "ITS2";
my $description = "ITS2 database collected from HMP data";
my $path = "/usr/local/scratch/its2_100bpFlank100pClus.fasta";

#public String uploadAndFormatBlastDataset(@WebParam(name="username")String username,
#                                              @WebParam(name="token")String token,
#                                              @WebParam(name="workSessionId")String workSessionId,
#                                              @WebParam(name="blastDBName")String blastDBName,
#                                              @WebParam(name="blastDBDescription")String blastDBDescription,
#                                              @WebParam(name="pathToFastaFile")String pathToFastaFile) throws RemoteException;
my $message = SOAP::Lite
	-> service( "http://saffordt-ws1:8180/compute-compute/ComputeWS?wsdl" )
	-> uploadAndFormatBlastDataset( $owner, "0", $workspace, $name, $description, $path );
print "VICS: $message\n";
	
exit(0);
