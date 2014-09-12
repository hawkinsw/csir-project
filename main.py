#!/usr/bin/python

from __future__ import print_function
from bs4 import BeautifulSoup
from doxygen import doxygen
import urllib2

def main():
	pd = doxygen.PotentialDoxygenSite("http://www.abisource.com/doxygen/")
	if pd.isDoxygenSite():
		d = doxygen.DoxygenSite(pd)
		#d.getFileList()
		d.getClassList()
		l = lambda x: print(x)
		d.forEachClass(l)
	else:
		print("No!")

if __name__ == "__main__":
	main()
