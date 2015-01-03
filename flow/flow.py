#!/usr/bin/python

import docdb
import re

# From http://stackoverflow.com/questions/1175208/elegant-python-function-to-convert-camelcase-to-camel-case
first_cap_re = re.compile('(.)([A-Z][a-z]+)')
all_cap_re = re.compile('([a-z0-9])([A-Z])')
no_numbers_re = re.compile('[0-9]')
def decamel(variable):
	v = first_cap_re.sub(r'\1_\2', variable)
	v = all_cap_re.sub(r'\1_\2', v)
	return no_numbers_re.sub(r'', v)

def splits(variable):
	# strip beginning and ending _s
	variable_ = variable.strip("_")
	variable_ = decamel(variable_)
	return variable_.split("_")

class Flow(object):
	def __init__(self, package_id, db):
		self.package_id = package_id
		self.db = db
	def calculate_flow(self):
		for source_id in self.db.sourceForPackage(self.package_id):
			#print(source_id)
			(source, method_name) = self.db.sourceForSource(source_id)
			self.visit(source_id, method_name, source)
