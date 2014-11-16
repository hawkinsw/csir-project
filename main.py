#!/usr/bin/python

from __future__ import print_function
from bs4 import BeautifulSoup
from doxygen import doxygen
import urllib2
from github import github
from github3 import models
from javadoc import javadoc
from git import Repo
import tempfile
import shutil
import docdb
#import logging

def get_runner(language,
	repo_source_name,
	repo_source_package,
	repo_source_url,
	repo_dir):
	if language == "Java":
		return javadoc.JavaDocRunner(repo_source_name, repo_source_package, repo_source_url, repo_dir)
	elif language == "C++":
		return doxygen.DoxygenRunner(repo_source_name, repo_source_package, repo_source_url, repo_dir)
	else:
		return None

def analyze(repo, repo_language, db):
	repo_dir = tempfile.mkdtemp()
	repo_source_name = str(repo)
	repo_source_package = repo.name
	repo_source_url = repo.git_url

	print("repo: " + repo_source_name)
	print("repo package: " + repo_source_package)
	print("repo url: " + repo_source_url)

	if db.packageExists(repo_source_name):
		print("Skipping " + repo_source_name + " because we've already seen it.")
		return

	remote = Repo.clone_from(repo_source_url, repo_dir)
	runner = get_runner(repo_language, repo_source_name, repo_source_package, repo_source_url, repo_dir)
	if runner == None:
		return
	runner.run(db.host, db.user, db.password, db.db)
	print(repo_dir)
	shutil.rmtree(repo_dir)

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

	db = docdb.DocDb("localhost", "ir", "ir", "ir")

	trending = github.GithubTrending("cpp")
	for trender in trending.repositories:
		print(trender)
		search = github.GithubSearch(str(trender), "C++", "", "")
		try:
			for r in search.search(2):
				analyze(r.repository, "C++", db)
		except models.GitHubError as ghe:
			print(ghe)

if __name__ == "__main__":
	test_search()
