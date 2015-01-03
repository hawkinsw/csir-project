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
	target = []
	arguments = []

	# In case the statement is a binary operation
	# v = lhs + rhs
	# v = lhs - rhs
	# etc
	# descend down the lhs and rhs:
	if hasattr(statement, 'lhs'):
		left = collect_variables(statement.lhs)
	if hasattr(statement, 'rhs'):
		right = collect_variables(statement.rhs)

	# Now, take care of different specific types
	# of statements.
	if type(statement) is plyj.model.Type:
		base = [str(statement.name)]

	elif type(statement) is plyj.model.InstanceCreation:
		base = collect_variables(statement.type)
		for ta in statement.type_arguments:
			arguments.extend(collect_variables(ta))
		for arg in statement.arguments:
			arguments.extend(collect_variables(arg))

	elif type(statement) is plyj.model.MethodInvocation:
		base = [str(statement.name)]
		target = collect_variables(statement.target)
		for arg in statement.arguments:
			arguments.extend(collect_variables(arg))

	elif type(statement) is plyj.model.Name:
		base = [str(statement.value)]

	# Combine what we have.
	base.extend(left)
	base.extend(right)
	base.extend(target)
	base.extend(arguments)
	return base

class JavaFlow(flow.Flow):
	def __init__(self, package_id, db):
		super(self.__class__, self).__init__(package_id, db)

	def visit(self, source_id, method_name, code):
		code = ("class A { void t() { %s }}" % (code))
		parser = plyj.parser.Parser()
		tree = parser.parse_string(code)
		package_id = self.package_id
		class JavaVisitor(plyj.model.Visitor):
			def visit_MethodDeclaration(self, method_decl):
				if method_decl.body is not None:
					for statement in method_decl.body:
						if type(statement) is plyj.model.Assignment:
							print(statement)
							word1 = None
							if type(statement.lhs) is plyj.model.Name:
								word1 = str(statement.lhs.value)
							elif hasattr(statement.lhs, 'name'):
								word1 = str(statement.lhs.name)
							words2 = []
							for v in collect_variables(statement.rhs):
								words2.extend(flow.splits(v))
							if word1 is not None and len(words2) != 0:
								words1 = flow.splits(word1)
								for word1 in words1:
									for word2 in words2:
										if word1.lower() != word2.lower():
											print(word1.lower() + " ?=> " + word2.lower())
											#db.addWordRelationship(package_id,
												#word1.lower(),
												#word2.lower())
		if tree != None:
			tree.accept(JavaVisitor())

if __name__ == "__main__":
	db = docdb.DocDb("localhost", "ir", "ir", "ir")
	db.connect()
	jf = JavaFlow(66, db)
	jf.calculate_flow()
	db.disconnect()
