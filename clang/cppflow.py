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

class CppFlow(flow.Flow):
	def visit(self, source_id, method_name, code):
		declarations = "";

		if code == "":
			return

		index = clang.cindex.Index.create()

		code = code[1::]
		tu = index.parse('t.c', unsaved_files=[('t.c', "int f() {" + code + "}")])

		for diag in tu.diagnostics:
			if diag.category_number == 2:
				spell = (str(diag.spelling)).split(' ')
				spell.reverse()
				new_declaration = re.sub('[\']', '', spell[0])
				if declarations != "":
					declarations += ","
				declarations = declarations + new_declaration
		declaration = "int " + declarations + ";"

		code = declaration + code
		tu = index.parse('t.c', unsaved_files=[('t.c', "int f() {" + code + "}")])

		def clang_flow(node):
			if node.kind == clang.cindex.CursorKind.BINARY_OPERATOR:
				var = None
				for token in node.get_tokens():
					if str(token.kind) == "TokenKind.IDENTIFIER":
						if var == None:
							var = token.spelling
						else:
							print(var + "=>" + token.spelling)
			for child in node.get_children():
				clang_flow(child)

		clang_flow(tu.cursor)

if __name__ == "__main__":
	db = docdb.DocDb("localhost", "ir", "ir", "ir")
	db.connect()
	cf = CppFlow(1, db)
	cf.calculate_flow()
	db.disconnect()
