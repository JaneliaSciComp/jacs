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

use warnings;
use strict;

use Getopt::Long;
use LWP::Simple;
use XML::Twig;
use Config::IniFiles;
use SOAP::Lite;
use Pod::Usage;

=head1  NAME

    vics_unit_test.pl - VICS Command-Line Interface for testing the services

=head1  USAGE

    vics_unit_test.pl   --config_ini_file /path/to/.ini/file
                       [--output /path/to/output/file
                        <wsdl options (see below)>
                        <list options (see below)>
                        --help]

=head1  OPTIONS

    --config_ini_file   -c  :   Path to the .ini style file containing parameters for the test runs

    --output_file       -o  :   Path to an output file.  Of not passed, all output goes to stdout.

    WSDL OPTIONS
    ---- -------
    --server        -s  :   The server on which the wsdl can be found
    --port          -p  :   The port through which the server should be channeled
    --wsdl_path     -w  :   The path on the server at which the wsdl is found
    --wsdl_url      -u  :   Complete url to the wsdl.  Overrides the preceeding three options.

    such that the values passed in for -s, -p, and -w get used to create -u as follows:
    http://<server>:<port>/<wsdl_path>
    Note that if the full url (-u) is given it should include the 'http://'.  Also, if the full
    url is given it makes no sense to use the other options, and they are ignored.

    LIST OPTIONS
    ---- -------
    --list_services     -l  :   List the service names for this wsdl.
    --show_params           :   Display the params for the given service[s]

    To display the names of the parameters for a single service:
            vics_cli.pl --show_params serviceName
    ... Or, for multiple services:
            vics_cli.pl --show_params serviceA,serviceB,serviceC
    ... Or even for ALL services:
            vics_cli.pl --show_params '*'



    --help  -h  :   Display this help

=head1  DESCRIPTION

    vics_unit_test allows the VICS SOAP services to be tested from a command-line.  

    It does this in the following manner:

    1.  Retrieve the service desriptions from the given wsdl.
    2.  Read a configuration file that defines:
        a: services to omit from testing (for example, cloud services, when in a grid environment)
        b: parameters for services to run
    3.  Run all services not in the list of services to be omitted with the appropriate options,
        reporting back any 'endpoint' messages, that is, error or complete status messages.


=head1  CONTACT

    Jason Inman
    jinman@jcvi.org

=cut

my $server = 'saffordt-ws1';
my $port = 81;
my $wsdl_path = 'compute-compute/ComputeWS?wsdl';
my $wsdl_url = "";

my @show_params;

my $config_ini_file;
my $cfg;    # Will become the Config::INI object
my $output_file;
my $ofh;    # Will become the output file handle.  STDOUT or some path.

my %opts;
my $result = GetOptions( \%opts,
                        'server|s=s',
                        'port|p=i',
                        'wsdl_path|w=s',
                        'wsdl_url|u=s',
                        'config_ini_file|c=s',
                        'output_file|o=s',
                        'help|h',
                    ) || die "Terrible error!! Can't parse options!\n";
#pod2usage ( {-exitval => 0, -verbose => 1}) unless (%opts || @ARGV);
pod2usage ( {-exitval => 0, -verbose => 2}) if $opts{help};
&parse_opts;               
select $ofh;

# Get the listing or show params if asked.
&list_services if (defined $opts{list_services});
&show_params(@show_params) if (@show_params);

# Get the wsdl and parse the available services
my $wsdl = get($wsdl_url);
die "No wsdl found at $wsdl_url\n" unless $wsdl;
my %wsdl_services;
my $twig = new XML::Twig( TwigHandlers => { "operation" => \&found_service });
$twig->parse($wsdl);

# Read in the config file
$cfg = new Config::IniFiles( -file => $config_ini_file ); 
my @omitted_services = $cfg->val('unit_test_flags','omitted');

foreach my $service (sort {$a cmp $b} keys %wsdl_services) {
    # Parallelize this shnizzit.  Or not.  Whatevs.

    print "-------- Found service $service --------------\n";

    # Skip omitted ones:
    if (&check_service_omission($service,\@omitted_services))  {
        print "Skipping $service:\tFound on list of service to omit in $config_ini_file\n";
        print "------------------------\n";
        next;
    }

    # get params from the file:
    print "Getting params for $service ... ";
    my ($values,$order) = &get_config_args($service);
    print "Done.\n";

    # submit and wait for a response:
    print "Submitting $service ... ";
    my $submission_return_string = &submit_job($service,$values,$order);
    print "$submission_return_string\n";
    print "Done.\n";

    # get the job_id:
    my $taskId = &get_id_from_return_string($submission_return_string);

    # return the status:
    if ($taskId) {
        print "Waiting for $service to complete as task $taskId:\n";
        print &get_job_status($taskId);
    } else {
        print "No task Id found for $service.\n"
    }

    print "\n$service has completed.------------------------\n";

}

exit(0);

sub get_id_from_return_string {

    # Given a return string, attempt to find the task/job/session/whatever id
    my $ret_string = shift;
    my $id = '';
    $id = $1 if ($ret_string =~ /(?:Session Id:|job|File id:) (\d+)/);
    return $id;

}

sub get_job_status {

    my ($taskId) = @_;
    my $sleep = 1;

    my $status = '';
    my $statusText = '';
    while ($status !~ /error|completed/) {

        sleep ($sleep);

        $statusText = SOAP::Lite-> service($wsdl_url)->getTaskStatus(getlogin,'',$taskId);
        if ($statusText =~ /Status Type: (\S+)/s) {
            $status = $1;
            print '.'; 
        } elsif ($statusText =~ /There was a problem/) {
            return "$statusText\n";
        }
    }

    return $statusText;

}

sub submit_job {

    my ($command,$values,$order) = @_;

    # convert the option hash to an array of the values
    my @param_values;
    foreach my $opt ( sort {$order->{$a} <=> $order->{$b}} keys %{$values} ) {
        push @param_values,$values->{$opt};
    }

    # send it off.  Return the output
    return( SOAP::Lite-> service($wsdl_url)->$command(@param_values) );

}

sub get_config_args {

    my ($command) = @_;

    # Get a list of the params in the command's section
    my @param_list = $cfg->Parameters($command);
    my %config_param_values = ();

    my %order = ();
    my $order = 0;

    # Pull each value into the hash
    foreach my $param (@param_list) {

        $config_param_values{$param} = $cfg->val($command,$param);
        $order{$param} = $order;
        $order++;

    }

    # return refernces to the hashes
    return \%config_param_values,\%order;

}

sub check_service_omission {

    # return true if service is among those to be omitted:
    my ($service, $blacklist) = @_;

    foreach (@{$blacklist}) {
        return $service if ($service eq $_);
    }
    # if not found, return false:
    return 0;

}

sub show_params {

    # display parameter order for a given service/list of servicse
    my @services = @_;

    if ($services[0] eq '*') {

        print map {"$_ :\n\t". join("\n\t",@{$wsdl_services{$_}}) . "\n"} keys %wsdl_services; 

    } else {

        foreach my $service (@services) {

            if (defined $wsdl_services{$service}) {
                print "$service : \n\t" , join("\n\t",@{$wsdl_services{$service}}), "\n";
            } else {
                print "No service named $service found at $wsdl_url\n";
            }

        }

    }

    exit(0);

}

sub list_services {

    # dump a list of the service names;
    if (scalar keys %wsdl_services) {

        print map {"$_\n"} sort {$a cmp $b} keys %wsdl_services;

    } else {

        print "No services found at $wsdl_url\n";

    }

    exit(0);

}         

sub found_service {

    my ($twig, $service) = @_;

    # skip these... they don't contain useful information to us.
    return if $service->has_child('soap:operation');

    # add the service and it's parameter list to the hash of services
    my $service_name = $service->{att}->{name};
    my @param_order  = split(' ',$service->{att}->{parameterOrder});
    $wsdl_services{$service_name} = \@param_order;

}

sub parse_opts {

    my $errors = '';
    
    # Create the wsdl url
    $wsdl_url = $opts{wsdl_url} || 
                'http://' .( (defined $opts{server}) ? $opts{server} : $server )  . ':' .
                           ( (defined $opts{port})   ? $opts{port}   : $port   )  . '/' .
                           ( (defined $opts{wsdl_path})  ?   $opts{wsdl_path}  : $wsdl_path);

    
    if (defined $opts{config_ini_file}) {
        $config_ini_file = $opts{config_ini_file};
    } else {
        $errors .= "Must be given an --config_ini_file!\n";
    }

    if ($opts{output_file}) {
        open($ofh,"> $opts{'output_file'}") || die "Can't write to $opts{'output_file'}: $!\n";
    } else {
       $ofh = *STDOUT;
    }

    die $errors if $errors;

}
