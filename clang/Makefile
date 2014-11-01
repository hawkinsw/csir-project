CLANG_HOME := /home/hawkinsw/code/csir/llvm/install
LLVMCOMPONENTS=option
LLVMCONFIG=$(CLANG_HOME)/bin/llvm-config
LIBS=\
-lclangTooling\
-lclangFrontendTool\
-lclangFrontend\
-lclangDriver\
-lclangSerialization\
-lclangCodeGen\
-lclangParse\
-lclangSema\
-lclangStaticAnalyzerFrontend\
-lclangStaticAnalyzerCheckers\
-lclangStaticAnalyzerCore\
-lclangAnalysis\
-lclangARCMigrate\
-lclangRewriteFrontend\
-lclangRewrite\
-lclangEdit\
-lclangAST\
-lclangLex\
-lclangBasic

#	-I$(CLANG_HOME)/include
#	-L$(CLANG_HOME)/lib $(LIBS)
docker: docker.cpp Makefile
	$(CLANG_HOME)/bin/clang++ -o docker docker.cpp \
	-I$(shell $(LLVMCONFIG) --src-root)/tools/clang/include \
	-I$(shell $(LLVMCONFIG) --obj-root)/tools/clang/include \
	$(shell $(LLVMCONFIG) --cxxflags) \
	$(LIBS) \
	$(shell $(LLVMCONFIG) --libs) \
	$(shell $(LLVMCONFIG) --system-libs) \
	$(shell $(LLVMCONFIG) --ldflags --libs $(LLVMCOMPONENTS))
clean:
	rm -f docker

