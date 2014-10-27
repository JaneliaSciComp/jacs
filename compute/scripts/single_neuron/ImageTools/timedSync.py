#!/usr/bin/python

from __future__ import print_function
import os, time, sys

def getGbps(numbytes, secs): return numbytes*8.0/1000/1000/1000/secs

if len(sys.argv)!=4:
    script = os.path.basename(__file__)
    print("Usage: %s [rsync|cp] [source] [target]"%script, file=sys.stderr)
    sys.exit(1)

(script,cmd,source,target) = sys.argv

statinfo = os.stat(source)
numbytes = statinfo.st_size
st = time.time()
os.system("%s -a %s %s"%(cmd, source, target))
elapsed = time.time()-st

print("%i,%f,%f" % (numbytes, elapsed, getGbps(numbytes, elapsed)))


