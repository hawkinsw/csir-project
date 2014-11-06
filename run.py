#!/usr/bin/python

from __future__ import print_function
from javadoc import javadoc

def test():
	jd = javadoc.JavaDocRunner("Testing1", "Testing1.gz", "URL", "/home/hawkinsw/code/csir/project/javadoc/src/")
	jd.run("localhost", "ir", "ir", "ir")

if __name__ == "__main__":
	test()
