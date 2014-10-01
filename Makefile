
SHELL := /usr/bin/env bash

# This path is only accurate when calling with `make -f` directly
# In a shell-wrap situation (like the `cmma` script) it resolves to the temp file descriptor
MAKEFILE_PATH := $(dir $(abspath $(lastword $(MAKEFILE_LIST))))

# Tools/Executables
CMMA_JAVA ?= $(shell which java)
CMMA_JAR ?= $(shell which jar)
CMMA_MVN ?= $(shell which mvn)
CMMA_LEIN ?= $(shell which lein)
CMMA_BOOT ?= $(shell which boot)
# This allows developers to override the classpath fetch with another tool
# If blank, it will use the CMMA classpath fn;  See the `getclasspath` function below
CMMA_CLASSPATH_BIN ?=
CMMA_VERSION ?= 0.1.0-SNAPSHOT
#CLJ_VERSION ?= 1.6.0 # Not used anymore
# This classpath is used for internal tasks, not for applications
#CMMA_UBERJAR ?= $(MAKEFILE_PATH)/cmma-clj/target/cmma-0.1.0-SNAPSHOT-standalone.jar
CMMA_UBERJAR ?= $(HOME)/.m2/repository/ohpauleez/cmma/cmma-$(CMMA_VERSION)-standalone.jar
CMMA_CORE_CLASSPATH ?= $(CLASSPATH):$(CMMA_UBERJAR):.

# TODO: Figure out how best to stich this through the defines or run at the top level
#ifneq ("$(wildcard $(CMMA_UBERJAR))","")
#	echo "CMMA's Uberjar was not detected! Patch up the core CMMA Makefile";
#    echo "Current path: $(CMMA_UBERJAR)";
#	echo "Exiting..."; exit 1;
#endif
#ABORT := $(shell test -s $(CMMA_UBERJAR) || { $(error "CMMA Uberjar not detected. See core Makefile. Exiting...") } )

# CMMA Toggles
#  - The custom target file
CMMA_TARGETS ?= Makefile.cmma
#  - Should CMMA include custom targets or dispatch? [Yes]
CMMA_INCLUDE ?= 1
#  - The file where dependencies (in the Lein+Git style) are specified [project.edn]
CMMA_DEPS_FILE ?= project.edn
#  - Should CMMA try to look for a project.clj as a fallback? [Yes] : TODO
CMMA_LEINDEPS_FALLBACK ?= 1
#  - The default target/action/task: [clj]
#CMMA_DEFAULT_TARGET = clj

# Jars, Classes, NSes
CMMA_COMPILE_NSES ?=
CMMA_CLJ ?= clojure.main
CMMA_CLJ_COMPILER ?= clojure.lang.Compile
CMMA_COMPILE_OUT ?= target/classes
CMMA_JVM_ARGS ?=

# Auxiliary Functions
quiet = $(if $V, $1, @echo " $2"; $1)
very-quiet = $(if $V, $1, @$1)

# TODO: Take the tabs/spaces out, in case someone needs to compose these
define cljfn
	$(shell $(CMMA_JAVA) -cp $1 $(CMMA_CLJ) $2)
endef
define cljvmfn
	$(CMMA_JAVA) -cp $1 $(CMMA_JVM_ARGS) $(CMMA_CLJ) $2
endef
define cljcompilefn
	$(CMMA_JAVA) -cp $1 $(CMMA_JVM_ARGS) -Dclojure.compile.path=$(CMMA_COMPILE_OUT) -Dclojure.compiler.elide-meta='[:doc :file :line]' $(CMMA_CLJ_COMPILER) $2
endef
define mvncp
	$(CMMA_MVN) dependency:build-classpath | awk '/\[INFO\] Dependencies classpath:/{flag=1;next}/\[INFO\] --/{flag=0}flag{print}' | tr -d \\n
endef
define leincp
	$(CMMA_LEIN) classpath
endef
define cmmaclasspath
$(call cljfn, $(CMMA_CORE_CLASSPATH), -m cmma.classpath)
endef
# Classpath
#  If CMMA Included-targets file specifies an alternative binary/function to run,
#  use that to resolve and return the Classpath.
#  Otherwise, use CMMA's classpath support.
define getclasspath
$(if $(CMMA_CLASSPATH_BIN),$(shell $(CMMA_CLASSPATH_BIN)),$(call cmmaclasspath))
endef
define namespacesfn
    $(if $(CMMA_COMPILE_NSES),$(CMMA_COMPILE_NSES),$(shell find $1 -name "*.clj" -print | tr '/_' '.-' | awk '{ sub(/\.\./, ""); sub(/.clj/, ""); sub(/$2/, ""); print }'))
endef

RUN_ARGS := $(wordlist 2,$(words $(MAKECMDGOALS)),$(MAKECMDGOALS))
# TODO: Figure out how to pull out Dirs from RUN_ARGS
TARGET_DIRS = $(shell THE_DIRS=(); for xd in ($(RUN_ARGS)) \
			  do \
			  if [ -d xd ] then; \
			    $$THE_DIRS+=xd \
			  fi \
			  done \
			  printf $$THE_DIRS)

# Targets
# clj, classpath, repl, nrepl, compile, ns, makefile
# Future Targets
# jar, uberjar, install, pom

# This is to test out the target dir support, which isn't working : TODO
#.PHONY : dirz
#dirz:
#	@echo $(TARGET_DIRS)

.PHONY : clj
clj:
	$(call cljvmfn, $(call getclasspath), $(RUN_ARGS))

force: ;

.PHONY : classpath
classpath:
	@echo $(call getclasspath)

$(CMMA_COMPILE_OUT):
	@mkdir -p $(CMMA_COMPILE_OUT)

# TODO: At some point we'll want to pass in source paths
# TODO: This shouldn't be PHONY, it'd be better if it tracked the class files
.PHONY : compile
compile: $(CMMA_COMPILE_OUT)
	$(call cljcompilefn,$(call getclasspath),$(call namespacesfn,src,src.))

# TODO: At some point 'src.' should be an arg
.PHONY : maybe-namespaces
maybe-namespaces:
	@echo $(call namespacesfn,$(RUN_ARGS),src.)

.PHONY : ns
ns:
	$(call cljvmfn,$(call getclasspath),-m $(RUN_ARGS))

.PHONY : repl
repl:
	$(call cljvmfn,$(call getclasspath),-r)

.PHONY : nrepl
nrepl:
	$(call cljvmfn,$(CMMA_CORE_CLASSPATH):$(call getclasspath),-m cmma.nrepl)

# Which commands accept command line args?
ifneq ($(findstring $1, 'clj ns maybe-namespaces'), "")
	$(eval $(RUN_ARGS):;@:)
endif

# Notes:
# - There should be a check to see which leading args are dirs and apply the tail tasks to all of those dirs
# - CMMA should have a `dev` ns for useful dev tooling.  Copy your user.clj to start
# - There should be a new method for deps-handling, that snapshots all deps (including repo revisions), that is compatible with Maven (FUTURE)

# Extension
#  By default, Makefile.cmma is included, allowing users to
#  override, redefine, and extend targets, as well as use all core variables.
#  Optionally, A user can constrain CMMA to only dispatch tasks to Makefile.cmma.
ifeq ($(CMMA_INCLUDE), 1)
# Including overrides
-include $(CMMA_TARGETS)
# Ignore missing targets
%: force ;
else
# Extending tasks (dispatch only)
%: force
	@$(MAKE) -f $(CMMA_TARGETS) $@
endif

