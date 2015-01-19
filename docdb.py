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
		self.package_exists_query = ("SELECT id FROM package WHERE name=%s")
		self.source_for_package_query=("SELECT id "
			"FROM source "
			"WHERE package_id=%s "
			"AND type=\"method\"")
		self.code_for_source_query=("SELECT source, name FROM source WHERE id=%s")
		self.update_relationship_count_query=(
			"UPDATE synonyms "
			"SET count=count+1 "
			"WHERE (word1=%(word1)s and word2=%(word2)s) or "
			"(word1=%(word2)s and word2=%(word1)s) AND "
			"package_id=%(package_id)s")
		self.add_relationship_query=(
			"INSERT INTO synonyms "
			"(package_id, count, word1, word2) "
			"VALUES "
			"(%s, 1, %s, %s)")
		self.update_invocations_query=(
			"UPDATE source "
			"SET invocations=%(invocations)s "
			"WHERE id=%(source_id)s")
		self.update_variables_query=(
			"UPDATE source "
			"SET variables=%(variables)s "
			"WHERE id=%(source_id)s")
		return True
		
	def addInvocations(self, source_id, invocations):
		if not self.connected:
			return (None, None)
		cursor = self.connection.cursor()
		cursor.execute(self.update_invocations_query,
		{'source_id': source_id,
		 'invocations': invocations,})
		cursor.fetchone()
		cursor.close()
		self.connection.commit()
	
	def addVariables(self, source_id, variables):
		if not self.connected:
			return (None, None)
		cursor = self.connection.cursor()
		cursor.execute(self.update_variables_query,
		{'source_id': source_id,
		 'variables': variables,})
		cursor.fetchone()
		cursor.close()
		self.connection.commit()

	def addWordRelationship(self, package_id, word1, word2):
		if not self.connected:
			return (None, None)
		cursor = self.connection.cursor()
		cursor.execute(self.update_relationship_count_query,
		{'package_id': package_id,
		 'word1': word1,
		 'word2': word2,})

		cursor.fetchone()
		if cursor.rowcount == 0:
			insert_cursor = self.connection.cursor()
			insert_cursor.execute(self.add_relationship_query,
				(package_id, word1, word2))
			insert_cursor.close()
		cursor.close()
		self.connection.commit()

	def sourceForSource(self, source_id):
		source = None
		method_name = None
		if not self.connected:
			return (None, None)
		cursor = self.connection.cursor()
		cursor.execute(self.code_for_source_query, (source_id,))
		(source, method_name) = cursor.fetchone()
		cursor.close()

		return (source, method_name)

	def sourceForPackage(self, package_id):
		sources = []
		if not self.connected:
			return sources
		cursor = self.connection.cursor()
		cursor.execute(self.source_for_package_query, (package_id,))

		for (source,) in cursor:
			sources.append(source)
		cursor.close()

		return sources

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

		cursor.execute(self.package_exists_query, (package_name,))
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
