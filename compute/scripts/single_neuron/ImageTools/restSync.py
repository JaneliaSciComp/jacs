#!/usr/bin/python

from __future__ import print_function
import os, time, sys, commands

def getGbps(numbytes, secs): 
    return numbytes*8.0/1000/1000/1000/secs

def repeatUntilSuccess(cmd, maxTries=3):
    success = False
    c = 0
    while True:
        if c>=maxTries: break
        if c>0:
            print("Communication failure, retrying...")
        print(cmd)
        (retval,httpcode) = commands.getstatusoutput(cmd)
        print("Exit code %d, HTTP status code %s"%(retval,httpcode))
        if retval==0 and httpcode=='200':
            success = True
            break
        c += 1

    if success:
        print("Result: success")
    else:
        print("Result: failure after %d tries"%maxTries)

def getMd5sum(filepath):
    return commands.getstatusoutput("md5sum %s"%filepath)[1].split(" ")[0]

def httpGet(source, target):
    cmd = "curl -Is %s | awk '/X-Scal-Usermd/{print $2}'" % source
    storedMd5sum = commands.getstatusoutput(cmd)[1].strip()
    cmd = "curl -w %%{http_code} --silent -o %s -X GET %s" % (target, source)
    st = time.time()
    repeatUntilSuccess(cmd)
    elapsed = time.time()-st
    statinfo = os.stat(target)
    numbytes = statinfo.st_size
    md5sum = getMd5sum(target)
    if storedMd5sum != md5sum:
        print("Error: md5sum mismatch (%s != %s)"%(storedMd5sum,md5sum))
    else:
        print("Md5sum: %s"%md5sum)
    print("Timing: %i,%f,%f" % (numbytes, elapsed, getGbps(numbytes, elapsed)))

def httpPut(source, target):
    statinfo = os.stat(source)
    numbytes = statinfo.st_size
    st = time.time()
    md5sum = getMd5sum(source)
    print("Md5sum: %s"%md5sum)
    cmd = "curl -w %%{http_code} --silent -o /dev/null -X PUT -H \"x-scal-usermd: %s\" %s --data-binary @%s" % (md5sum, target, source)
    repeatUntilSuccess(cmd)
    elapsed = time.time()-st
    print("Timing: %i,%f,%f" % (numbytes, elapsed, getGbps(numbytes, elapsed)))

if __name__ == "__main__":

    if len(sys.argv)!=4:
        script = os.path.basename(__file__)
        print("Usage: %s [GET|PUT] [source] [target]"%script, file=sys.stderr)
        sys.exit(1)

    (script,cmd,source,target) = sys.argv
    if cmd=="GET":
        httpGet(source, target)
    elif cmd=="PUT":
        httpPut(source, target)
    else:
        print("Command must be GET or PUT.")
        sys.exit(1)

