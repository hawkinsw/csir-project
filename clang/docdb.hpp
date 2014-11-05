#ifndef DOCKER_MYSQL_H
#define DOCKER_MYSQL_H

#include <string>

class DocDbMysqlWrapper;

class DocDb {
	public:
		DocDb(std::string mysqlHost,
			std::string mysqlUser,
			std::string mysqlPass,
			std::string mysqlDb);

		bool connect();
		bool disconnect();
		int addPackage(std::string name, std::string filename, std::string url);
		int addSource(int packageId, std::string type, std::string name, std::string code);
		int addDocumentation(int packageId, int sourceId, std::string documentation);
		int getPackageIdFromName(std::string name);
		int getSourceIdFromName(int packageId, std::string name);
		bool updateSource(int packageId, int sourceId, std::string source);

	private:
		bool prepareQueries();

		DocDbMysqlWrapper *m_wrapper;
};

#endif
