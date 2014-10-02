
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

#### The "easy" way

Edit any paths in the `install` script and run it.  This script automates the
hard way.  This way assumes you'll use a central Makefile (see below)

#### The "hard" way

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

See the [examples](./cmma-clj/examples) directory for `Makefile.cmma` examples
and using CMMA with lein, maven, or as a standalone tool.

#### What about profiles/plugins/some-other-lein-feature?

You're using Make now, so all Make functionality holds.  "Profiles" are just
other Makefiles.  Plugins/Extensions morph into targets you write and include.

Additionally CMMA can sit on top of whatever tools you want - Leiningen, Maven,
Gradle, whatever.  At the top of the `Makefile`, you'll see some binaries
supplied for you - CMMA\_LEIN, CMMA\_MVN, etc.  Any binary you can call,
anything you can do at the shell, you can do in here.  You also have a
mechanism on how to talk about task dependencies.

CMMA's extra functionality is written as a library of Clojure functions -
you can use CMMA directly from any REPL and control it programmatically.

#### What about CMMA's deps management?

As stated above, you can use any tool you'd like to manage dependencies.
CMMA also can manage your dependencies for you.

CMMA sits on top of Pomegranate to do Maven deps, but also opens deps up to
tagged literals, and ships with a way to define dependencies via Git.
Git deps are essentially Leiningens Checkouts, but with a way to lock down branch/commit/tag
and communicate those to others (ie: Checkouts are local only and can't be communicated to other team members).

Dependencies are placed in `project.edn`.  If a `project.edn` file isn't located,
and you haven't told CMMA to use another tool to resolve the classpath,
the classpath defaults to the Clojure 1.6 jar (resolved in your `~/.m2`),
and the current working directory (`.`).

#### Working with the `project.edn` file

The top-level `:app` holds project metadata.  Some of these fields will be used
in jar generation.  Developers should feel free to add new keys within this map
for their own purposes.

Your project's `:dependencies` follow the same format found in Leiningen, with
the addition that you can also use tagged literals.

Additionally, you can specify `:dev-dependencies` - tools and libraries used for
your day-to-day work.  By default, these are included for all tasks except for
`jar` and `uberjar` (which do no currently exist).  If this is unacceptable for
you, can change the behavior by overriding the classpath command.

CMMA runs in "pedantic" mode.  That is, if any version conflicts or ranges are
found, it will abort (and tell you, rather cryptically, how to fix it).

In CMMA, the word is exactly how you see it - nothing is hidden from you.
For this reason, you must always specify your `:respositories`, `:src-paths`,
`:resource-paths`, and `:test-paths`.

Currently, `:nrepl-options` can be set in the project.edn file, but that will
most likely change.  In the future, nREPL settings will be passed in via the
command line (or captured in your Makefile.cmma).  As it stands, `:nrepl-options`
are optional.

