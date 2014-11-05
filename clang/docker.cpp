#include "clang/Tooling/Tooling.h"
#include "clang/Tooling/CommonOptionsParser.h"
#include "llvm/Support/CommandLine.h"
#include "clang/AST/AST.h"
#include "clang/AST/ASTContext.h"
#include "clang/AST/ASTConsumer.h"
#include "clang/AST/RecursiveASTVisitor.h"
#include "clang/Frontend/FrontendActions.h"
#include "clang/Frontend/CompilerInstance.h"
#include <iostream>
#include "docdb.hpp"

using namespace std;
using namespace clang;
using namespace clang::driver;
using namespace clang::tooling;
using namespace llvm;

static cl::OptionCategory DockerCategory("Docker options");

static cl::opt<string> MysqlUser("mysqluser",
	cl::desc("Username for Mysql"),
	cl::cat(DockerCategory),
	cl::Required);
static cl::opt<string> MysqlPass("mysqlpass",
	cl::desc("Passname for Mysql"),
	cl::cat(DockerCategory), cl::Required);
static cl::opt<string> MysqlHost("mysqlhost",
	cl::desc("Hostname for Mysql"),
	cl::cat(DockerCategory),
	cl::Required);
static cl::opt<string> MysqlDb("mysqldb",
	cl::desc("Dbname for Mysql"),
	cl::cat(DockerCategory),
	cl::Required);
static cl::opt<string> SourceName("sourcename",
	cl::desc("Name of the source package"),
	cl::cat(DockerCategory),
	cl::Required);
static cl::opt<string> SourceUrl("sourceurl",
	cl::desc("Source package url"),
	cl::cat(DockerCategory),
	cl::Required);
static cl::opt<string> SourcePackage("sourcepackage",
	cl::desc("Source package filename"),
	cl::cat(DockerCategory),
	cl::Required);

class DockerVisitor : public RecursiveASTVisitor<DockerVisitor> {
	public:
		DockerVisitor(int packageId, DocDb *db) {
			m_packageId = packageId;
			m_docDb = db;
		}

		bool VisitDecl(Decl *d) {
			LangOptions langOptions;
			PrintingPolicy pp(langOptions);

			if (d->isFunctionOrFunctionTemplate()) {
				int sourceId = -1;
				FunctionDecl *fd = d->getAsFunction();
				DeclarationNameInfo dni = fd->getNameInfo();
				cout << "Adding function " << dni.getAsString() << endl;
				if (fd->hasBody()) {
					string functionBodyString;
					raw_string_ostream functionBodyStringStream(functionBodyString);
					Stmt *functionBody = fd->getBody();

					functionBody->printPretty(functionBodyStringStream, NULL, pp);

					/*
					 * Assume that we are running this first. That means we are
					 * required to insert the source rather than update it.
					 */

					if ((sourceId = m_docDb->addSource(m_packageId, "method", dni.getAsString(), functionBodyStringStream.str())) == -1) {
						cout << "Error occurred adding source for function " << dni.getAsString() << endl;
					}
				}
			}
			return true;
		}

	private:
		DocDb *m_docDb;
		int m_packageId;
};

class DockerASTConsumer : public ASTConsumer {
	private:
		DockerVisitor *visitor;
	public:
		DockerASTConsumer(CompilerInstance *CI, int packageId, DocDb *db) {
			visitor = new DockerVisitor(packageId, db);
		}

		virtual bool HandleTopLevelDecl(DeclGroupRef DG) {
			for (DeclGroupRef::iterator it = DG.begin(); it!=DG.end(); it++) {
				Decl *d = *it;
				visitor->TraverseDecl(d);
			}
			return true;
		}
};

class DockerFrontendAction : public ASTFrontendAction {
	public:
		DockerFrontendAction(int packageId, DocDb *db) {
			m_packageId = packageId;
			m_docDb = db;
		}

		virtual std::unique_ptr<ASTConsumer> CreateASTConsumer(CompilerInstance &CI, StringRef file) {
			return std::unique_ptr<ASTConsumer>(new DockerASTConsumer(&CI, m_packageId, m_docDb));
		}
	private:
		int m_packageId;
		DocDb *m_docDb;
};

class DockerFrontendActionFactory : public FrontendActionFactory {
	public:
		DockerFrontendActionFactory(int packageId, DocDb *db) {
			m_packageId = packageId;
			m_docDb = db;
		}
		FrontendAction *create() override {
			return new DockerFrontendAction(m_packageId, m_docDb);
		}

	private:
		int m_packageId;
		DocDb *m_docDb;
};

int main(int argc, const char *argv[]) {
	int toolResult = 1;
	int packageId = -1;
	CommonOptionsParser op(argc, argv, DockerCategory);
	ClangTool Tool(op.getCompilations(), op.getSourcePathList());
	DocDb *db = new DocDb(MysqlHost, MysqlUser, MysqlPass, MysqlDb);

	if (!db->connect()) {
		return 1;
	}

	if ((packageId = db->addPackage(SourceName, SourcePackage, SourceUrl))==-1) {
		db->disconnect();
		return 1;
	}

	toolResult = Tool.run(new DockerFrontendActionFactory(packageId, db));

	db->disconnect();
	return toolResult;
}
