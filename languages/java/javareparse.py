#!/usr/bin/python
from __future__ import print_function

# NB: Install plyj first.
import plyj.parser
import plyj.model
from flow import flow
import docdb
from languages.java import parseutils

class JavaReparse:
	def __init__(self, package_id, db):
		self.package_id = package_id
		self.db = db

	def reparse(self):
		for source_id in self.db.sourceForPackage(self.package_id):
			#print(source_id)
			(source, method_name) = self.db.sourceForSource(source_id)
			self.visit(source_id, method_name, source)

	def visit(self, source_id, method_name, code):
		code = ("""class A { void t() { %s }}""" % code)
		parser = plyj.parser.Parser()
		tree = parser.parse_string(code)
		package_id = self.package_id
		invocations = {}
		variables = {}
		invocation_names = []
		variable_names = []
		class JavaVisitor(plyj.model.Visitor):
			def visit_MethodDeclaration(self, method_decl):
				if method_decl.body is not None:
					for statement in method_decl.body:
						for v in parseutils.collect_variables(statement):
							variables[v] = 1
						for i in parseutils.collect_invocations(statement):
							invocations[i] = 1

		visitor = JavaVisitor()
		if tree != None:
			tree.accept(visitor)
		invocation_names = invocations.keys()
		variable_names = variables.keys()
		variable_names.sort()
		invocation_names.sort()
		print("Method: " + method_name)
		print("id: " + str(source_id))
		self.db.addInvocations(source_id, ",".join(invocations.keys()))
		self.db.addVariables(source_id, ",".join(variables.keys()))
		print("variables: " + ",".join(variable_names))
		print("invocations: " + ",".join(invocation_names))

if __name__ == "__main__":
	db = docdb.DocDb("localhost", "ir", "ir", "ir")
	db.connect()
	jf = JavaReparse(1, db)
	jf.reparse()
	jf = JavaReparse(2, db)
	jf.reparse()
	jf = JavaReparse(3, db)
	jf.reparse()
	jf = JavaReparse(4, db)
	jf.reparse()
	jf = JavaReparse(5, db)
	jf.reparse()
	db.disconnect()
