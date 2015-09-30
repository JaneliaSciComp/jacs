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
use SOAP::Lite;
use Pod::Usage;

=head1  NAME

    vics_cli.pl - VICS Command-Line Interface

=head1  SYNOPSIS

    vics_cli allows the VICS SOAP services to be called from a command-line.  In addition,
    basic exploration of the wsdl is allowed (Listing available services, and listing parameters to services).

    To list the available services in a wsdl:

            vics_cli.pl --list_services

    To display the names of the parameters for a service:

            vics_cli.pl --show_params serviceName

    ... Or, for multiple services:

            vics_cli.pl --show_params serviceA,serviceB,serviceC

    ... Or even for ALL services:

            vics_cli.pl --show_params '*'

    To run a service, enter the wrapper options (like the wsdl_path, etc.) then use " -- " to
    begin entering the serviceName and it's agruments.

            vics_cli.pl -- serviceName --option1 option1_valus --option2 -o option3_value

    Running a service will print the return string to stdout.

    Specifying the wsdl can be done with a variety of options:

    --server    | -s
    --port      | -p
    --wsdl_path | -w  
    --wsdl_url  | -u

    such that the values passed in for -s, -p, and -w get used to create -u as follows:

    http://<server>:<port>/<wsdl_path>

    Note that if the full url (-u) is given it should include the 'http://'.  Also, if the full
    url is given it makes no sense to use the other options, and they are ignored.

=head1  CONTACT

    Jason Inman
    jinman@jcvi.org

=cut

my $server = 'saffordt-ws1';
my $port = 81;
my $wsdl_path = 'compute-compute/ComputeWS?wsdl';
my $wsdl_url;

my @show_params;
my $run_service;
my @service_args;

my %opts;
my $result = GetOptions( \%opts,
                        'server|s=s',
                        'port|p=i',
                        'wsdl_path|w=s',
                        'wsdl_url|u=s',
                        'list_services|l',
                        'show_params=s',
                        'help|h',
                    ) || die "Terrible error!! Can't parse options!\n";
pod2usage ( {-exitval => 0, -verbose => 2}) unless (%opts || @ARGV);
pod2usage ( {-exitval => 0, -verbose => 2}) if $opts{help};
&parse_opts;               

# Get the wsdl and parse the available services
my $wsdl = get($wsdl_url);
die "No wsdl found at $wsdl_url\n" unless $wsdl;

my %wsdl_services;
my $twig = new XML::Twig( TwigHandlers => { "operation" => \&found_service });
$twig->parse($wsdl);

&list_services if (defined $opts{list_services});
&show_params(@show_params) if (@show_params);
&submit_command if ($run_service);

exit(0);

sub submit_command {

    print "Submitting $run_service\n";

    if (defined $wsdl_services{$run_service}) {

        # turn the array of service params into a hash (and remember the correct order)
        my %arg_order;
        my $order = 0;
        my %run_service_args;
        foreach my $arg (@{$wsdl_services{$run_service}}) {
            $run_service_args{$arg} = '';
            $arg_order{$arg} = $order++;
        }

        # Send in any user-defined options.
        my $arg_name = '';
        foreach my $arg (@service_args) {
            # If the value was 'attached' to the key with an '=' make sure we grab it
            # Otherwise, just store the key name and wait to see if a value comes in.
            # 'valueless' parameters (flags) get a value of 1 for the hash.  
            if ($arg =~ /^-{1,2}(.*)(=(.+))?/) {

                # check against known params;
                die "No paramter named $1 in parameter list for $run_service in $wsdl_url!\n" unless (exists $run_service_args{$1});

                # if $arg_name is still defined it means it's a flag.
                if ($arg_name) {
                    $run_service_args{$arg_name}++;
                }

                # if we have the argument value being toted along with '='
                if ($2) {
                    $run_service_args{$1} = $3;
                    $arg_name = '';
                } else {
                # otherwise just set the upcoming argument to the first capture
                    $arg_name = $1;
                }

            # here we handle 'bare' arguments (which for vics services MUST be associated with a parameter name)
            } elsif ($arg_name) {

                $run_service_args{$arg_name} = $arg;
                $arg_name = '';

            } else {
                die "Found value with no preceeding argument: $arg\n";
            }
        }

        # handle possible flag in the last spot:
        $run_service_args{$arg_name}++ if ($arg_name);
        
        # Username should be set to the logged in user if it hasn't been passed in.
        if (exists $arg_order{username}) {
            $run_service_args{username} = getlogin() unless $run_service_args{username};
        }

        # We must have a project, also... don't make any assumptions here...
        if (exists $arg_order{project}) {
            die "Aborting because there was no project code given!\n" unless ($run_service_args{project});
        }

        # Now arrayify the arguments:
        my @ordered_args;
        foreach my $arg (sort {$arg_order{$a} <=> $arg_order{$b}} keys %run_service_args) {
            push @ordered_args, $run_service_args{$arg};
        }
        
        # call the service now
        print SOAP::Lite-> service ($wsdl_url)->$run_service(@ordered_args);
        print "\n";

    } else {

        print "No service named $run_service found at $wsdl_url\n";

    }

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

    if (defined $opts{show_params}) {

        @show_params = split(',',$opts{show_params});

    }

    # handle getting the service and service parameters
    ($run_service, @service_args) = @ARGV;

    die $errors if $errors;

}
