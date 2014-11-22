#!/usr/bin/python

import docdb

class Flow(object):
	def __init__(self, package_id, db):
		self.package_id = package_id
		self.db = db
	def calculate_flow(self):
		for source_id in self.db.sourceForPackage(self.package_id):
			print(source_id)
			(source, method_name) = self.db.sourceForSource(source_id)
			self.visit(source_id, method_name, source)
