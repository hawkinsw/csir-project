#!/usr/bin/python

from subprocess import call
from os.path import walk
__ant_command__=["/usr/bin/ant", "-f","/home/hawkinsw/code/csir/project/javadoc/run.xml"]

class JavaDocRunner:
	def __init__(self, source_name, source_package, source_url, source_path):
		self.source_name = source_name
		self.source_package = source_package
		self.source_url = source_url
		self.source_path = source_path

	def run(self, db_host, db_user, db_pass, db_db):
		def dir_visitor(arg, directory, files):
			if directory.endswith("src"):
				dock_command = __ant_command__ + ["dock",
					"-Dsource=" + directory,
					"-Dsource_name=" + self.source_name,
					"-Dsource_package=" + self.source_package,
					"-Dsource_url=" + self.source_url,
					"-Ddb_host=" + db_host,
					"-Ddb_user=" + db_user,
					"-Ddb_pass=" + db_pass,
					"-Ddb_db=" + db_db]
				print(" ".join(dock_command))
				call(dock_command)
		walk(self.source_path, dir_visitor, None)

		# Now, call source for each source file.
		def dir_visitor(arg, directory, files):
			for f in files:
				if f.endswith("java"):
					source_command = __ant_command__ + ["source",
					"-Dsource=" + directory + "/" + f,
					"-Dsource_name=" + self.source_name,
					"-Dsource_package=" + self.source_package,
					"-Dsource_url=" + self.source_url,
					"-Ddb_host=" + db_host,
					"-Ddb_user=" + db_user,
					"-Ddb_pass=" + db_pass,
					"-Ddb_db=" + db_db]
					print(" ".join(source_command))
					call(source_command)
		walk(self.source_path, dir_visitor, None)

def main():
	pass

if __name__ == "__main__":
	main()
