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
};

class DockerASTConsumer : public ASTConsumer {
	private:
		DockerVisitor *visitor;
	public:
		DockerASTConsumer(CompilerInstance *CI) {
		}
		virtual bool HandleTopLevelDecl(DeclGroupRef DG) {
			for (DeclGroupRef::iterator it = DG.begin(); it!=DG.end(); it++) {
			/*
			 * This will not be good enough! Top level declarations could
			 * be classes, and we want to descend into those and look
			 * for functions there too! But, it's a start.
			 */
				Decl *d = *it;
				if (d->isFunctionOrFunctionTemplate()) {
					FunctionDecl *fd = d->getAsFunction();
					DeclarationNameInfo dni = fd->getNameInfo();
					cout << "Function : " << dni.getAsString() << endl;
				}
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
