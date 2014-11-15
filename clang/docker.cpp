#include "clang/Tooling/Tooling.h"
#include "clang/Basic/TargetInfo.h"
#include "clang/Tooling/CommonOptionsParser.h"
#include "clang/Parse/ParseAST.h"
#include "llvm/Support/CommandLine.h"
#include "llvm/Support/Host.h"
#include "clang/Sema/Sema.h"
#include "clang/Lex/Preprocessor.h"
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
static cl::opt<string> SourceFile("file",
	cl::desc("Source filename"),
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
			Decl::Kind k = d->getKind();

			if (d->isFunctionOrFunctionTemplate()) {
				int sourceId = -1;
				FunctionDecl *fd = d->getAsFunction();
				string functionName, functionType;

				functionType = fd->getReturnType().getAsString();
				functionName = fd->getQualifiedNameAsString();

				if (functionName.find(string("std::")) != std::string::npos) {
					cout << "Skipping function " << functionName << endl;
					return true;
				}

				cout << "Adding function " << functionName << endl;
				if (fd->hasBody()) {
					string functionBodyString;
					raw_string_ostream functionBodyStringStream(functionBodyString);
					Stmt *functionBody = fd->getBody();

					functionBody->printPretty(functionBodyStringStream, NULL, pp);

					/*
					 * Assume that we are running this first. That means we are
					 * required to insert the source rather than update it.
					 */

					if ((sourceId = m_docDb->addSource(m_packageId,
						"method",
						functionType,
						functionName,
						functionBodyStringStream.str())) == -1) {
						cout << "Error occurred adding source for function " 
						     << functionName << endl;
					}
				}
				if (sourceId != -1) {
					/* insert parameters. */
					for (FunctionDecl::param_iterator pi = fd->param_begin(); 
					     pi != fd->param_end();
						   pi++) {
						ParmVarDecl *pvd = *pi;
						m_docDb->addParameter(m_packageId, sourceId, pvd->getOriginalType().getAsString(), pvd->getNameAsString());
					}
				}
			} else if (k == clang::Decl::Namespace) {
				NamespaceDecl *nsd = cast<NamespaceDecl>(d);
				cout << "Namespace decl: " << nsd->getQualifiedNameAsString() << endl;
			} else if (k == clang::Decl::UsingDirective) {
				string namespaceName;
				raw_string_ostream namespaceNameStream(namespaceName);
				UsingDirectiveDecl *usd = cast<UsingDirectiveDecl>(d);
				const NamespaceDecl *nsd = usd->getNominatedNamespace()->getOriginalNamespace();
				nsd->getNameForDiagnostic(namespaceNameStream, pp, true);
				cout << "Using : " << namespaceNameStream.str() << endl;
			}
			return true;
		}

	private:
		DocDb *m_docDb;
		int m_packageId;
};

class NamespaceTypoProvider : public clang::ExternalSemaSource {
	Sema *CurrentSema;

public:
	NamespaceTypoProvider()
			: CurrentSema(nullptr) {}

	virtual void InitializeSema(Sema &S) { CurrentSema = &S; }

	virtual void ForgetSema() { CurrentSema = nullptr; }

	virtual TypoCorrection CorrectTypo(const DeclarationNameInfo &Typo,
																		 int LookupKind, Scope *S, CXXScopeSpec *SS,
																		 CorrectionCandidateCallback &CCC,
																		 DeclContext *MemberContext,
																		 bool EnteringContext,
																		 const ObjCObjectPointerType *OPT) {
		if (CurrentSema && LookupKind == Sema::LookupNamespaceName) {
			DeclContext *DestContext = nullptr;
			ASTContext &Context = CurrentSema->getASTContext();
			if (SS)
				DestContext = CurrentSema->computeDeclContext(*SS, EnteringContext);
			if (!DestContext)
				DestContext = Context.getTranslationUnitDecl();
			IdentifierInfo *ToIdent =
					CurrentSema->getPreprocessor().getIdentifierInfo(
					Typo.getName().getAsString());
			NamespaceDecl *NewNamespace =
					NamespaceDecl::Create(Context, DestContext, false, Typo.getBeginLoc(),
																Typo.getLoc(), ToIdent, nullptr);
			DestContext->addDecl(NewNamespace);
			TypoCorrection Correction(ToIdent);
			Correction.addCorrectionDecl(NewNamespace);
			return Correction;
		}
		return TypoCorrection();
	}
};

class DockerASTConsumer : public ASTConsumer {
	private:
		DockerVisitor *visitor;
	public:
		DockerASTConsumer() {
			visitor = NULL;
		}

		DockerASTConsumer(CompilerInstance *CI, int packageId, DocDb *db) {
			visitor = new DockerVisitor(packageId, db);
		}
		/*
		void HandleTranslationUnit(ASTContext &c) {
			DeclContext *dc = c.getTranslationUnitDecl();
			for (DeclContext::decl_iterator it = dc->decls_begin();
			     it != dc->decls_end();
					 it++) {
				Decl *d = *it;
				visitor->TraverseDecl(d);
			}
		}
		*/
		virtual bool HandleTopLevelDecl(DeclGroupRef DG) {
			for (DeclGroupRef::iterator it = DG.begin(); it!=DG.end(); it++) {
				Decl *d = *it;
				visitor->TraverseDecl(d);
			}
			return true;
		}
};

int main(int argc, const char *argv[]) {
	int toolResult = 1;
	int packageId = -1;
	CommonOptionsParser op(argc, argv, DockerCategory);
	CompilerInstance ci;
	SourceLocation sl = SourceLocation();
	std::shared_ptr<TargetOptions> to_shared = std::shared_ptr<TargetOptions>(new TargetOptions());
	TargetOptions *to = to_shared.get();
	TargetInfo *ti;
	LangOptions los;
	FileID fileID;
	NamespaceTypoProvider *nss = new NamespaceTypoProvider();
	DocDb *db = new DocDb(MysqlHost, MysqlUser, MysqlPass, MysqlDb);
	const FileEntry *file;

	if (!db->connect()) {
		return 1;
	}

	if ((packageId = db->addPackage(SourceName, SourcePackage, SourceUrl))==-1) {
		db->disconnect();
		return 1;
	}

	to->Triple = llvm::sys::getDefaultTargetTriple();

	ci.createDiagnostics();
	ti = TargetInfo::CreateTargetInfo(ci.getDiagnostics(),to_shared);

	ci.setTarget(ti);

	/*
	 * Turn on C++ support in the parser.
	 */
	los = ci.getLangOpts();
	los.CPlusPlus = 1;
	ci.getLangOpts() = los;

	ci.createFileManager();
	ci.createSourceManager(ci.getFileManager());
	ci.createPreprocessor(clang::TU_Complete);
	ci.createASTContext();

	ci.setASTConsumer(std::unique_ptr<ASTConsumer>(
		new DockerASTConsumer(&ci, packageId, db)));

	ci.createSema(clang::TU_Complete, nullptr);
	ci.getSema().addExternalSource(nss);

	file = ci.getFileManager().getFile(SourceFile.c_str());
	fileID = ci.getSourceManager().createFileID(file, sl, clang::SrcMgr::C_User);
	ci.getSourceManager().setMainFileID(fileID);

	ci.getDiagnosticClient().BeginSourceFile(
		ci.getLangOpts(), &ci.getPreprocessor());

	nss->InitializeSema(ci.getSema());
	clang::ParseAST(ci.getSema(), false, false);

	ci.getDiagnosticClient().EndSourceFile();

	db->disconnect();

	return 1;
}
