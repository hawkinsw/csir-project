#!/usr/bin/python

from __future__ import print_function
from bs4 import BeautifulSoup
from doxygen import doxygen
import urllib2
from github import github
from github3 import models
#import logging

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

def test_search():
	#file_handler = logging.FileHandler("main.py.log")
	#logger = logging.getLogger('github3')
	#logger.addHandler(file_handler)
	#logger.setLevel(logging.DEBUG)

	search = github.GithubSearch("test", "js", "", "")
	try:
		for r in search.search():
			print(r.repository)
	except models.GitHubError as ghe:
		print(ghe)

if __name__ == "__main__":
	test_search()
