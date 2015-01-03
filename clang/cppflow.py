#!/usr/bin/python
from __future__ import print_function

# NB: Must set PYTHONPATH before this will work.
import clang.cindex
from flow import flow
import docdb
import re

def find_compound_statement(cursor):
	if cursor.kind == clang.cindex.CursorKind.COMPOUND_STMT:
		return cursor
	for child in cursor.get_children():
		compound = find_compound_statement(child)
		if compound is not None:
			return compound
	return None

def collect_variables(statement):
	variables = []
	print(statement.spelling)
	for c in statement.walk_preorder():
		if str(c.kind) == "CursorKind.DECL_REF_EXPR":
			print(str(c.spelling))
			variables.append(c.spelling)
		else:
			print(str(c.kind))
			print(str(c.spelling))
	return variables

def clang_flow(node):
	is_lhs = True
	lhs = ""
	print(str(node.kind))
	if node.kind == clang.cindex.CursorKind.BINARY_OPERATOR or \
		node.kind == clang.cindex.CursorKind.COMPOUND_ASSIGNMENT_OPERATOR:
		for c in node.get_children():
			if is_lhs:
				# convert into a suitable lhs
				lhs = str(c.spelling)
				is_lhs = False
				pass
			else:
				print("lhs: " + lhs)
				collect_variables(c)
		print("-----")
#				var = None
#				values = []
#				for token in node.get_tokens():
#					for t in token.get_children():
#						print("child: " + str(t))
#					print(str(token.kind))
#					if str(token.kind) == "TokenKind.IDENTIFIER":
#						if var == None:
#							var = token.spelling
#						else:
#							values.append(token.spelling)
#				if len(values) != 0:
#					print(var + "=>" + str(values))


def print_tokens(token_generator):
	t = " "
	for token in token_generator:
		t += str(token.spelling) + " "
	print(t)

class CppFlow(flow.Flow):
	def visit(self, source_id, method_name, code):
		declarations = {};
		members = {};

		if code == "":
			return

		index = clang.cindex.Index.create()

		code = code[1::]

		# Tentatively parse.
		tu = index.parse('t.cpp', args=["-fsyntax-only"], unsaved_files=[('t.cpp', "int f() {" + code + "}")])

		#print("Orig:")
		#print(code)
		# Go through the parser output and 
		# fixup any undeclared variables 
		# by simply declaring them as integers.
		for diag in tu.diagnostics:
			print(str(diag.spelling))
			if diag.category_number == 2:
				spell = (str(diag.spelling)).split(' ')
				spell.reverse()
				new_declaration = re.sub('[\']', '', spell[0])
				if new_declaration not in declarations:
					declarations[new_declaration] = 1;
		i = 0
		declaration = ""
		classes = {}
		for d in declarations:
			classes[("a%d"%i)] = d
			i+=1
		for c in classes:
			declaration += ("class %s {}; %s %s;" % (c,c,classes[c]))

		# Really parse.
		#code = declaration + code
		tu = index.parse('t.cpp', args=["-fsyntax-only"], unsaved_files=[('t.cpp', "int f() {" + declaration + code + "}")])
		print(declaration + code)	
		for diag in tu.diagnostics:
			print(str(diag.spelling))
		for child in (find_compound_statement(tu.cursor)).get_children():
			clang_flow(child)

if __name__ == "__main__":
	db = docdb.DocDb("localhost", "ir", "ir", "ir")
	db.connect()
	cf = CppFlow(3, db)
	cf.calculate_flow()
	db.disconnect()
