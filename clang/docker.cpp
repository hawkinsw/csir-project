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
		DockerVisitor(int packageId, DocDb *db, SourceManager *sm, LangOptions *lo) {
			m_packageId = packageId;
			m_docDb = db;
			m_sourceManager = sm;
			m_langOpts = lo;
			m_dependencies = new vector<string>();
		}

		bool VisitDecl(Decl *d) {
			PrintingPolicy pp(*m_langOpts);
			Decl::Kind k = d->getKind();

			if (d->isFunctionOrFunctionTemplate()) {
				int sourceId = -1;
				FunctionDecl *fd = NULL;
				string functionName, functionType;
				Stmt *functionBody = NULL;
				CharSourceRange functionRange;
				StringRef source;
				unsigned functionParamCount;

				fd = d->getAsFunction();
				functionType = fd->getReturnType().getAsString();
				functionName = fd->getQualifiedNameAsString();
				functionParamCount = fd->getNumParams();

				/*
				 * Skip either std:: functions or those without
				 * implementations.
				 */
				if (functionName.find(string("std::")) != std::string::npos ||
				    !fd->hasBody()) {
					cout << "Skipping function " << functionName << endl;
					return true;
				}

				functionBody = fd->getBody();
				functionRange = CharSourceRange::getCharRange(
					functionBody->getSourceRange());
				source = clang::Lexer::getSourceText(
					functionRange,
					*m_sourceManager,
					*m_langOpts);

				/*
				 * Add or update source.
				 */
				sourceId = m_docDb->getSourceIdFromName(m_packageId,
					functionName,
					functionParamCount);
				if (sourceId == -1) {
					cout << "Adding function " << functionName << endl;
					if ((sourceId = m_docDb->addSource(m_packageId,
						"method",
						functionType,
						functionName,
						functionParamCount,
						source.str())) == -1) {
						cout << "Error occurred adding source for function " 
						     << functionName << endl;
					}
				} else {
					cout << "Updating function " << functionName << endl;
					m_docDb->updateSource(m_packageId, sourceId, source.str());
				}

				if (sourceId != -1) {
					/* insert parameters. */
					for (FunctionDecl::param_iterator pi = fd->param_begin(); 
					     pi != fd->param_end();
						   pi++) {
						ParmVarDecl *pvd = *pi;
						m_docDb->addParameter(m_packageId,
							sourceId,
							pvd->getOriginalType().getAsString(),
							pvd->getNameAsString());
					}
					for (vector<string>::iterator it = m_dependencies->begin();
					     it != m_dependencies->end();
							 it++) {
						string dependencyName = *it;
						cout << "Adding dependency on " << dependencyName << endl;
						m_docDb->addDependencyName(m_packageId, sourceId, dependencyName);
					}
				}
			} else if (k == clang::Decl::Namespace) {
				NamespaceDecl *nsd = NULL;
				string namespaceName;

				nsd = cast<NamespaceDecl>(d);
				namespaceName = nsd->getQualifiedNameAsString();
				cout << "Namespace decl: " << namespaceName << endl;
				if (m_docDb->addSource(m_packageId,
					"namespace",
					"",
					namespaceName,
					0,
					"") == -1) {
						cout << "Error occurred adding namespace " << namespaceName << endl;
					}
			} else if (k == clang::Decl::UsingDirective) {
				UsingDirectiveDecl *usd = NULL;
				NamespaceDecl *nsd = NULL;
				string usingWhat;

				usd = cast<UsingDirectiveDecl>(d);
				nsd = usd->getNominatedNamespace()->getOriginalNamespace();
				usingWhat = nsd->getQualifiedNameAsString();

				m_dependencies->push_back(usingWhat);
			}
			return true;
		}

	private:
		DocDb *m_docDb;
		SourceManager *m_sourceManager;
		LangOptions *m_langOpts;
		vector<string> *m_dependencies;
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
			visitor = new DockerVisitor(packageId, db, &CI->getSourceManager(), &CI->getLangOpts());
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

	packageId = db->getPackageIdFromName(SourceName);
	if (packageId == -1 && (packageId = db->addPackage(SourceName, SourcePackage, SourceUrl))==-1) {
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
