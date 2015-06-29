(defproject datascript "0.11.5"
  :description "An implementation of Datomic in-memory database and Datalog query engine in ClojureScript"
  :license {:name "Eclipse"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :url "https://github.com/tonsky/datascript"

  :dependencies [
    [org.clojure/clojure "1.7.0-RC2" :scope "provided"]
    [org.clojure/clojurescript "0.0-3308" :scope "provided"]
    [org.clojure/core.async "0.1.346.0-17112a-alpha"]
  ]

  :global-vars {
    *warn-on-reflection* true
;;     *unchecked-math* :warn-on-boxed
  }

  :jvm-opts ["-Xmx2g" "-server"]

  :aliases {"test-clj"     ["run" "-m" "datascript.test/test-most"]
            "test-clj-all" ["run" "-m" "datascript.test/test-all"]
            "noderepl"     ["run" "-m" "clojure.main" "repl.clj"]}

  :cljsbuild {
    :builds [
      { :id "release"
        :source-paths ["src" "bench/src"]
        :assert false
        :compiler {
          :output-to     "release-js/datascript.bare.js"
          :optimizations :advanced
          :pretty-print  false
          :elide-asserts true
          :output-wrapper false
          :warnings      {:single-segment-namespace false}
        }
        :notify-command ["release-js/wrap_bare.sh"]}
  ]}

  :profiles {
    :dev {
      :source-paths ["bench/src" "test"]
      :plugins [
        [lein-cljsbuild "1.0.6"]
      ]
      :cljsbuild {
        :builds [
          { :id "advanced"
            :source-paths ["src" "bench/src" "test"]
            :compiler {
              :main          datascript.test
              :output-to     "target/datascript.js"
;;               :output-dir    "target/advanced"
              :optimizations :advanced
              :source-map    "target/datascript.js.map"
              :pretty-print  true
              :warnings     {:single-segment-namespace false}
              :recompile-dependents false
            }}
          { :id "none"
            :source-paths ["src" "bench/src" "test"]
            :compiler {
              :main          datascript.test
              :output-to     "target/datascript.js"
              :output-dir    "target/none"
              :optimizations :none
              :source-map    true
              :warnings     {:single-segment-namespace false}
              :recompile-dependents false
            }}
        ]
      }
    }
  }

  :clean-targets ^{:protect false} [
    "target"
    "release-js/datascript.bare.js"
    "release-js/datascript.js"
  ]
)
