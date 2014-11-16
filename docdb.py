#!/usr/bin/python

import mysql.connector
from mysql.connector import errorcode

class DocDb:
	def __init__(self, host, user, password, db):
		self.host = host
		self.user = user
		self.password = password
		self.db = db
		self.connected = False

	def __prepare(self):
		self.unresolved_query = ("SELECT DISTINCT(depends_on_name) AS dependency "
			"FROM dependency WHERE "
			"depends_on_name!=\"\"")
		self.package_exists = ("SELECT id FROM package WHERE name=%s")

		return True

	def connect(self):
		try:
			self.connection = mysql.connector.connect(user=self.user,
				password=self.password,
				host=self.host,
				database=self.db)
			self.connected = True
			if not self.__prepare():
				self.disconnect()
				self.connected = False
		except mysql.connector.Error as err:
			self.connection = None
			self.connected = False
		return self.connected

	def packageExists(self, package_name):
		exists = False
		if not self.connected:
			return exists
		cursor = self.connection.cursor()

		cursor.execute(self.package_exists, (package_name,))
		cursor.fetchone()
		if cursor.rowcount!=-1:
			exists = True
		else:
			exists = False

		cursor.close
		return exists

	def getUnresolvedDependencies(self):
		unresolved = []
		if not self.connected:
			return unresolved
		cursor = self.connection.cursor()
		cursor.execute(self.unresolved_query)

		for (dependency,) in cursor:
			unresolved.append(dependency)
		cursor.close()

		return unresolved

	def disconnect(self):
		if self.connected:
			self.connection.close()
		return True

def test_docdb():
	db = DocDb("localhost", "ir", "ir", "ir")
	if db.connect():
		print("Connected!\n")
	else:
		print("Not connected.\n")

	for d in db.getUnresolvedDependencies():
		print(d)

	if db.packageExists("testing"):
		print("testing exists.")
	else:
		print("testing does not exist.")

	db.disconnect()
	pass

if __name__ == "__main__":
	test_docdb()
