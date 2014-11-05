CLANG_HOME := /home/hawkinsw/code/csir/llvm/install
MYSQL_INCLUDES=-I/usr/include/mysql/ -I/usr/include/mysql++/
MYSQL_LIBS=-lmysqlpp -lmysqlclient
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

docker: docker.cpp Makefile docdb.o
	$(CLANG_HOME)/bin/clang++ -o docker docker.cpp docdb.o \
	-I$(shell $(LLVMCONFIG) --src-root)/tools/clang/include \
	-I$(shell $(LLVMCONFIG) --obj-root)/tools/clang/include \
	$(shell $(LLVMCONFIG) --cxxflags) \
	-fexceptions \
	$(LIBS) \
	$(shell $(LLVMCONFIG) --libs) \
	$(shell $(LLVMCONFIG) --system-libs) \
	$(shell $(LLVMCONFIG) --ldflags --libs $(LLVMCOMPONENTS)) \
	$(MYSQL_LIBS)

docdb.o: docdb.cpp docdb.hpp Makefile
	g++ -std=c++11 $(MYSQL_INCLUDES) -c docdb.cpp

docdb_test: docdb.cpp docdb_test.cpp
	g++ -std=c++11 $(MYSQL_INCLUDES) -o docdb_test docdb.cpp docdb_test.cpp $(MYSQL_LIBS)

test:
	./docker -mysqluser ir -mysqlpass ir -mysqldb ir -mysqlhost localhost -sourcename testing -sourcepackage package.tar.gz -sourceurl http://www.github.com/ docker.cpp --

clean:
	rm -f docker

