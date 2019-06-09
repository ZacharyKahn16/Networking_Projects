#!/bin/bash
java -cp ".:libftp.jar" cpsc441.a3.client.FastFtpDriver -s localhost -p 2525 -w 10 -t 1000 -f "FastFtpDriver.java"
