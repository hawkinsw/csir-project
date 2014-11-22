#!/usr/bin/python
from __future__ import print_function

# NB: Install plyj first.
import plyj.parser
import plyj.model
from flow import flow
import docdb

def collect_variables(statement):
	left = []
	right = []
	base = []
	print(str(statement))
	if hasattr(statement, 'lhs'):
		left = collect_variables(statement.lhs)
	if hasattr(statement, 'rhs'):
		right = collect_variables(statement.rhs)
	if hasattr(statement, 'name'):
		base = [str(statement.name)]
	elif type(statement) is plyj.model.Name:
		base = [str(statement.value)]
	base.extend(left)
	base.extend(right)
	return base

class JavaFlow(flow.Flow):
	def __init__(self, package_id, db):
		super(self.__class__, self).__init__(package_id, db)

	def visit(self, source_id, method_name, code):
		code = ("class A { void t() { %s }}" % (code))
		parser = plyj.parser.Parser()
		tree = parser.parse_string(code)
		relateds = {}
		class JavaVisitor(plyj.model.Visitor):
			def visit_MethodDeclaration(self, method_decl):
				if method_decl.body is not None:
					for statement in method_decl.body:
						if type(statement) is plyj.model.Assignment:
							relateds[str(statement.lhs.value)] = collect_variables(statement.rhs)
		tree.accept(JavaVisitor())

if __name__ == "__main__":
	db = docdb.DocDb("localhost", "ir", "ir", "ir")
	db.connect()
	jf = JavaFlow(1, db)
	jf.calculate_flow()
	db.disconnect()
