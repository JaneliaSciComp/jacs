#/bin/sh
#
# Run the two-stage neuron separation
#
# Usage:
#
# sh separate.sh <output dir> <name> <input file> 

DIR=$(cd "$(dirname "$0")"; pwd)

NSDIR="$DIR/../install/neusep-redhat"
OUTDIR=$1
NAME=$2
INPUTFILE=$3

#Usage: sampsepNALoadRaw  [-c<double(5.0)>] [-e<double(3.5)>] [-s<int(300)>] 
#                         [-gp] [-pj] [-nr] [-fx]
#                          <folder:FOLDER>   <core:STRING>   <inputs:FILE> ...

SAMPSEP="$NSDIR/sampsepNALoadRaw -nr -pj $OUTDIR $NAME $INPUTFILE"
echo "$SAMPSEP"
$SAMPSEP

