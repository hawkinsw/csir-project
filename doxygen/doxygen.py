#!/usr/bin/python

from subprocess import call
from os.path import walk
__doxygen_command__=["/home/hawkinsw/code/csir/project/doxygen/doxygen.pl"]
__doxygen_prepare_command__=["/home/hawkinsw/code/csir/project/doxygen/prepare_doxygen.pl"]

class DoxygenRunner:
	def __init__(self, source_name, source_package, source_url, source_path):
		self.source_name = source_name
		self.source_package = source_package
		self.source_url = source_url
		self.source_path = source_path

	def run(self, db_host, db_user, db_pass, db_db):
		doxygen_prepare_command = __doxygen_prepare_command__ + [self.source_name,
			self.source_package,
			self.source_url,
			self.source_path,
			db_host,
			db_user,
			db_pass,
			db_db]
		print(" ".join(doxygen_prepare_command))
		call(doxygen_prepare_command)

		doxygen_command = __doxygen_command__ + [self.source_name,
			self.source_package,
			self.source_url,
			self.source_path,
			db_host,
			db_user,
			db_pass,
			db_db]
		print(" ".join(doxygen_command))
		call(doxygen_command)

		# Now, call source for each source file.
		def dir_visitor(arg, directory, files):
			for f in files:
				if f.endswith("h") or f.endswith("hpp") or \
				   f.endswith("cpp") or f.endswith("cc") or \
				   f.endswith("cxx"):
					source_command = __doxygen_command__ + [self.source_name,
						self.source_package,
						self.source_url,
						self.source_path,
						db_host,
						db_user,
						db_pass,
						db_db]
#					print(" ".join(source_command))
#					call(source_command)

		walk(self.source_path, dir_visitor, None)

def main():
	pass

if __name__ == "__main__":
	main()
