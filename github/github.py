#!/usr/bin/python

from bs4 import BeautifulSoup
from doxygen import Doxygen
import urllib2
from github3 import GitHub

class GithubSearch:
	def __init__(self, query, language, user, password):
		self.query = query
		self.language = language
		self.user = user
		self.password = password
		self.g = GitHub(self.user, self.password)

	def search(self):
		return self.g.search_repositories(self.query + " language:" + self.language, number=2)

class GithubTrending:
	TrendingBaseUrl = "http://github.com/trending"

	def __init__(self, language=None):
		self.repositories = []
		trending_url = GithubTrending.TrendingBaseUrl
		if not language == None:
			trending_url += "?l=" + language
		response = urllib2.urlopen(trending_url)
		soup = BeautifulSoup(response.read())

		projects = soup.find_all("h3", class_="repo-list-name")
		for project in projects:
			projectUrl = project.find("a")['href']
			(_, projectUser, projectRepo) = projectUrl.split("/", 2)
			self.repositories.append(GithubRepository(projectUser, projectRepo))

		response.close

class GithubRepository:
	RepositoryUrlBase = "http://www.github.com/"
	def __init__(self, user, repository):
		self.user = user
		self.repository = repository

	def __repr__(self):
		return self.user + "/" + self.repository

	def getHtml(self):
		url = GithubRepository.RepositoryUrlBase + \
			"/" + self.user + \
			"/" + self.repository
		response = urllib2.urlopen(url)
		soup = BeautifulSoup(response.read())
		response.close()
		return soup.prettify()

def main():
	GithubTrending()

if __name__ == "__main__":
	main()
