#include "docdb.hpp"
#include <iostream>

using namespace std;

int main() {
	int packageId = -1, sourceId = -1, namespaceId;
	DocDb docDb("localhost", "ir", "ir", "ir");

	if (docDb.connect()) {
		cout << "Connected successfully!" << endl;
	} else {
		cout << "Could not connect!" << endl;
	}

	cout << (packageId = docDb.addPackage("name", "filename", "url")) << endl;
	cout << (sourceId = docDb.addSource(packageId, "method", "int", "test::test1", "return 0;")) << endl;
	cout << (namespaceId = docDb.addSource(packageId, "namespace", "", "scope", "")) << endl;
	cout << docDb.addDocumentation(packageId, sourceId, "Documentation1") << endl;
	cout << packageId << " =? " << docDb.getPackageIdFromName("name") << endl;
	cout << sourceId << " =? " << docDb.getSourceIdFromName(packageId, "test::test1") << endl;
	if (docDb.updateSource(packageId, sourceId, "return 1;"))
		cout << "Update succeeded!" << endl;
	else
		cout << "Update failed!\n" << endl;

	cout << docDb.addParameter(packageId, sourceId, "p1", "n1") << endl;

	cout << docDb.addDependencyName(packageId, sourceId, "scope") << endl;
	cout << docDb.addDependencyName(packageId, sourceId, "scape") << endl;
	
	cout << (namespaceId = docDb.addSource(packageId, "namespace", "", "scape", "")) << endl;
	cout << docDb.updateDependency("scape", namespaceId) << endl;
	docDb.disconnect();

	return 1;
}
