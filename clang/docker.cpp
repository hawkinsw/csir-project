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

using namespace std;
using namespace clang;
using namespace clang::driver;
using namespace clang::tooling;
using namespace llvm;

static cl::OptionCategory DockerCategory("Docker options");
static cl::extrahelp CommonHelp("This is common help.");

class DockerVisitor : public RecursiveASTVisitor<DockerVisitor> {
	public:
	bool VisitDecl(Decl *d) {
		LangOptions langOptions;
		PrintingPolicy pp(langOptions);

		if (d->isFunctionOrFunctionTemplate()) {
			FunctionDecl *fd = d->getAsFunction();
			DeclarationNameInfo dni = fd->getNameInfo();
			cout << "Function : " << dni.getAsString() << endl;
			if (fd->hasBody()) {
				string functionBodyString;
				raw_string_ostream functionBodyStringStream(functionBodyString);
				Stmt *functionBody = fd->getBody();
				functionBody->printPretty(functionBodyStringStream, NULL, pp);
				cout << functionBodyStringStream.str() << endl;
			}
		}
		return true;
	}
};

class DockerASTConsumer : public ASTConsumer {
	private:
		DockerVisitor *visitor;
	public:
		DockerASTConsumer(CompilerInstance *CI) {
			visitor = new DockerVisitor();
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
		virtual std::unique_ptr<ASTConsumer> CreateASTConsumer(CompilerInstance &CI, StringRef file) {
			return std::unique_ptr<ASTConsumer>(new DockerASTConsumer(&CI));
		}
};

int main(int argc, const char *argv[]) {
	CommonOptionsParser op(argc, argv, DockerCategory);
	ClangTool Tool(op.getCompilations(), op.getSourcePathList());

	int result = Tool.run(newFrontendActionFactory<DockerFrontendAction>().get());

	return result;
}
