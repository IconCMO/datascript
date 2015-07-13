A couple notes from the development process. This is current as of 2015-07-13, but, as the ClojureScript development processis happening pretty rapidly…who knows what will or will not work when you touch this? ;-)

A couple items of special note:

## Installation

Install [Leiningen](http://leiningen.org/), often via `brew install leiningen` or your platform's package manager. Dependencies and the like are defined in `project.clj` similar to `package.json`.

## Tests

Currently, the only tests we care about (and will actually pass) are in `test/datascript/test/async.cljc`. The other tests won't pass due to all the `core.async` additions. This could probably be solved by an extra layer around `core.async` if we every figured that out.

To run just that file for testing, I recommend running `(require 'datascript.test.async :reload)` from within a REPL (see below).

## `lein cljsbuild once release`

This will build the `release/datascript.js` file. Then, from within `release-js`, you can `npm publish` to update the version on npm.

## REPL

You can actually get a command-line into your current program. While there are a bunch of ways the ClojureScript wiki says about setting up a REPL, we found the easiest was a Node.js REPL with a bit of a helper script. To start it, run:

`rlwrap lein noderepl`

- The `rlwrap` appears to give you up-arrow abilities on the command line.
- `lein` is the main Leiningen command.
- `noderepl` is an alias defined in `project.clj` to run the `repl.clj` script.

This will automatically re-compile source file changes, which you can then reload into the environment via something like `(require 'datascript.file :reload)`. You can then either use the re-defined functionality (i.e. `(execute-a-function parameter)`) or rerun tests or whatever.

If you want to examine stuff inside a namespace, you can switch into a namespace's scope via (in-ns 'datascript.test.async). This **doesn't** automatically load a file or anything, but after loading the file, you can examine functions and data in that namespace's scope directly.


## Debugging

`(println "stuff")` should work in all the files.

Also, the ClojureScript wiki talks about installing a source map library (`npm install source-map-support`) which can help, though I didn't find it super useful. Your milage may vary.

## ClojureScript Wiki

https://github.com/clojure/clojurescript/wiki/Quick-Start

## Macros

Since all the files are in `*.cljc` format, one needs to be tricky about requiring macros and whatnot, since macros are defined in Clojure, not ClojureScript, and are required in ClojureScript, not Clojure. FYI.
