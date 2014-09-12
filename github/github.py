#!/usr/bin/python

from bs4 import BeautifulSoup
from doxygen import Doxygen
import urllib2

class GithubTrending:
	TrendingBaseUrl = "http://github.com/trending"

	def __init__(self):
		self.repositories = []
		response = urllib2.urlopen(GithubTrending.TrendingBaseUrl)
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
