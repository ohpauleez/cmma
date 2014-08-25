
Clojure and Make Managed Applications
=====================================

![](docs/commas_save_lives.jpg)

### A fair warning

This is not made for public consumption.  Feel free to dig through or even
try cmma out, but everything is very much in flux.

At some point a final rationale and install/usage directions will appear here.

### License

Copyright © 2014 Paul deGrandis

Distributed under the [Eclipse Public License 1.0](http://opensource.org/licenses/EPL-1.0)

### In-progress notes

#### How to install

First, make an uberjar of cmma-clj, and patch up the path the cmma standalone
jar in the Makefile (`CMMA_UBERJAR`).

You have two install options.

 * You can copy the `Makefile` into every project you want to use it in, and use `make` directly.
 * You can use one copy of the `Makefile` for all projects.  See directions below

1. Place the `Makefile` somewhere on your system (I suggest in your home directory, `~/.cmma/`, for example).
2. Place the `cmma` script in your path (optional, just makes calling the Makefile easier)
3. Patch up paths in `cmma` to the location of the Makefile (`DIR`), if needed
4. You're done.

#### How to use

You can override and customize CMMA's behaviors on a per-project basis with a Makefile.cmma file (placed in the base/root of your project).
This is just a Makefile that CMMA includes if it's found.

If you are copying the `Makefile` per project, just use `make` as you normally would.

If you're using the shell script, treat `cmma` like you would `make`.

#### Some examples

 * `cmma compile` - Compile namespaces to classes ahead of time
 * `cmma repl` - Launch a Clojure REPL
 * `cmma nrepl` - Launch an nREPL
 * `cmma classpath` - Echo the classpath
 * `cmma clj` - Call "clojure.main"
 * `cmma clj -m cmma.classpath` - Run the `-main` in the "cmma.classpath" namespace
 * `cmma ns cmma.classpath` - Same as above
 * `cmma makefile` - echo out the core Makefile

#### What about profiles/plugins/some-other-lein-feature?

You're using Make now, so all Make functionality holds.  "Profiles" are just
other Makefiles.  Plugins/Extensions morph into targets you write and include.

Additionally CMMA can sit on top of whatever tools you want - Leiningen, Maven,
Gradle, whatever.  At the top of the `Makefile`, you'll see some binaries
supplied for you - CMMA\_LEIN, CMMA\_MVN, etc.  Any binary you can call,
anything you can do at the shell, you can do in here.  You also have a
mechanism on how to talk about task dependencies.

#### What about CMMA's deps management?

CMMA sits on top of Pomegranate to do Maven deps, but also opens deps up to
tagged literals, and ships with a way to define dependencies via Git.
Git deps are essentaially Leiningens Checkouts, but a way to lock down branch/commit/tag
and communicate those to others (ie: Checkouts are local only and can't be communicated to other team members).

More to come...

