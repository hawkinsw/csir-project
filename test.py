#!/usr/bin/python

from __future__ import print_function
from javadoc import javadoc
from doxygen import doxygen

def testJavaDoc():
	jd = javadoc.JavaDocRunner("Testing1", "Testing1.gz", "URL", "/home/hawkinsw/code/csir/project/javadoc/src/")
	jd.run("localhost", "ir", "ir", "ir")

def testDoxygen():
	dx = doxygen.DoxygenRunner("Testing1", "Testing1.gz", "URL", "/tmp/gnuradio/gnuradio-3.7.5.1")
	dx.run("localhost", "ir", "ir", "ir")


if __name__ == "__main__":
	testDoxygen()
