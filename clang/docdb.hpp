#ifndef DOCKER_MYSQL_H
#define DOCKER_MYSQL_H

#include <string>
#include <mysql++/mysql++.h>

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

	private:
		bool prepareQueries();

		std::string m_mysqlUser;
		std::string m_mysqlPass;
		std::string m_mysqlHost;
		std::string m_mysqlDb;

		bool m_isConnected;

		mysqlpp::DBDriver *m_driver;
		mysqlpp::Connection *m_con;
		mysqlpp::Query *m_addPackageQuery,
			*m_addSourceQuery,
			*m_addDocumentationQuery,
			*m_getPackageIdQuery,
			*m_getSourceIdQuery;
};

#endif
