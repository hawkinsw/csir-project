#!/usr/bin/python
from __future__ import print_function

# NB: Install plyj first.
import plyj.parser
import plyj.model

#
# NOTE: There is LOTS of duplicated code here.
# For now it works, but this could be MUCH better.
#

def collect_invocations(statement):
	left = []
	right = []
	base = []
	target = []
	arguments = []

	#print(statement)
	#print("\n")

	# In case the statement is a binary operation
	# v = lhs + rhs
	# v = lhs - rhs
	# etc
	# descend down the lhs and rhs:
	if hasattr(statement, 'lhs'):
		left = collect_invocations(statement.lhs)
	if hasattr(statement, 'rhs'):
		right = collect_invocations(statement.rhs)

	# Now, take care of different specific types
	# of statements.
	if hasattr(statement, 'body'):
		if type(statement.body) is list:
			for s in statement.body:
				base.extend(collect_invocations(s))
		else:
			base.extend(collect_invocations(statement.body))

	if type(statement) is plyj.model.Try:
		base = collect_invocations(statement.block)
		right = collect_invocations(statement._finally)
	
	elif type(statement) is plyj.model.While:
		base.extend(collect_invocations(statement.predicate))
		# body already handled above.

	elif type(statement) is plyj.model.IfThenElse:
		base.extend(collect_invocations(statement.predicate))
		left = collect_invocations(statement.if_true)
		right = collect_invocations(statement.if_false)

	elif type(statement) is plyj.model.Block:
		for s in statement.statements:
			base.extend(collect_invocations(s))

	elif type(statement) is plyj.model.Type:
		#base = [str(statement.name)]
		pass

	elif type(statement) is plyj.model.InstanceCreation:
		for ta in statement.type_arguments:
			arguments.extend(collect_invocations(ta))
		for arg in statement.arguments:
			arguments.extend(collect_invocations(arg))

	elif type(statement) is plyj.model.MethodInvocation:
		base.append(str(statement.name))
		base.extend(collect_invocations(statement.target))
		for arg in statement.arguments:
			arguments.extend(collect_invocations(arg))

#	elif type(statement) is plyj.model.Name:
#		base = [str(statement.value)]

	elif type(statement) is plyj.model.VariableDeclaration:
		for decl in statement.variable_declarators:
			base.extend(collect_invocations(decl.initializer))

	# Combine what we have.
	base.extend(left)
	base.extend(right)
	base.extend(target)
	base.extend(arguments)
	return base


def collect_variables(statement):
	left = []
	right = []
	base = []
	target = []
	arguments = []

	#print(statement)
	#print("\n")

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
	if hasattr(statement, 'body'):
		if type(statement.body) is list:
			for s in statement.body:
				base.extend(collect_variables(s))
		else:
			base.extend(collect_variables(statement.body))

	if type(statement) is plyj.model.Try:
		base = collect_variables(statement.block)
		right = collect_variables(statement._finally)
	
	elif type(statement) is plyj.model.While:
		base.extend(collect_variables(statement.predicate))
		# body already handled above.

	elif type(statement) is plyj.model.IfThenElse:
		base.extend(collect_variables(statement.predicate))
		left = collect_variables(statement.if_true)
		right = collect_variables(statement.if_false)

	elif type(statement) is plyj.model.Block:
		for s in statement.statements:
			base.extend(collect_variables(s))

	#elif type(statement) is plyj.model.Type:
		#base = [str(statement.name)]

	elif type(statement) is plyj.model.InstanceCreation:
		for ta in statement.type_arguments:
			arguments.extend(collect_variables(ta))
		for arg in statement.arguments:
			arguments.extend(collect_variables(arg))

	elif type(statement) is plyj.model.MethodInvocation:
		base = collect_variables(statement.target)
		for arg in statement.arguments:
			arguments.extend(collect_variables(arg))

	elif type(statement) is plyj.model.Name:
		base = [str(statement.value)]

	elif type(statement) is plyj.model.VariableDeclaration:
		for decl in statement.variable_declarators:
			base.append(decl.variable.name)
			base.extend(collect_variables(decl.initializer))

	# Combine what we have.
	base.extend(left)
	base.extend(right)
	base.extend(target)
	base.extend(arguments)
	return base
