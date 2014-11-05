#include "docdb.hpp"
#include <iostream>

using namespace std;

int main() {
	int packageId = -1, sourceId = -1;
	DocDb docDb("localhost", "ir", "ir", "ir");

	if (docDb.connect()) {
		cout << "Connected successfully!" << endl;
	} else {
		cout << "Could not connect!" << endl;
	}

	cout << (packageId = docDb.addPackage("name", "filename", "url")) << endl;
	cout << (sourceId = docDb.addSource(packageId, "method", "int", "test1", "return 0;")) << endl;
	cout << docDb.addDocumentation(packageId, sourceId, "Documentation1") << endl;
	cout << packageId << " =? " << docDb.getPackageIdFromName("name") << endl;
	cout << sourceId << " =? " << docDb.getSourceIdFromName(packageId, "test1") << endl;
	if (docDb.updateSource(packageId, sourceId, "return 1;"))
		cout << "Update succeeded!" << endl;
	else
		cout << "Update failed!\n" << endl;

	cout << docDb.addParameter(packageId, sourceId, "p1", "n1") << endl;
	
	docDb.disconnect();

	return 1;
}
