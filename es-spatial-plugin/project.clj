(def version
  "Parses the version out of this file to use in names of referenced files"
  (let [project-clj-lines (-> "project.clj" slurp (clojure.string/split #"\n"))]
    (-> (filter (partial re-find #"^\(defproject") project-clj-lines)
        first
        (clojure.string/split #" ")
        last
        (clojure.string/replace "\"" ""))))

(def uberjar-name
  (str "target/cmr-es-spatial-plugin-" version "-standalone.jar"))

(def plugin-zip-name
  (str "target/cmr-es-spatial-plugin-" version ".zip"))

(defproject nasa-cmr/cmr-es-spatial-plugin "0.1.0-SNAPSHOT"
  :description "A Elastic Search plugin that enables spatial search entirely within elastic."
  :url "***REMOVED***projects/CMR/repos/cmr/browse/es-spatial-plugin"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.elasticsearch/elasticsearch "1.4.5"]

                 ;; Version set to match elastic search numbers. Look in elasticsearch pom.xml
                 [log4j/log4j "1.2.17"]
                 [nasa-cmr/cmr-spatial-lib "0.1.0-SNAPSHOT"]]

  :plugins [[lein-shell "0.4.0"]
            [lein-test-out "0.3.1"]]

  ;; This is the minimum that must be AOT'd for running in an embeded elastic. AOT :all for installing
  ;; in an elastic vm.
  :aot [cmr.es-spatial-plugin.SpatialScript
        cmr.es-spatial-plugin.SpatialScriptFactory
        cmr.es-spatial-plugin.SpatialSearchPlugin]

  :profiles
  {:dev {:dependencies [[nasa-cmr/cmr-elastic-utils-lib "0.1.0-SNAPSHOT"]
                        [org.clojure/tools.namespace "0.2.10"]
                        [org.clojars.gjahad/debug-repl "0.3.3"]
                        [criterium "0.4.3"]]

         :global-vars {*warn-on-reflection* true
                       *assert* false}

         ;; The ^replace is done to disable the tiered compilation for accurate benchmarks
         ;; See https://github.com/technomancy/leiningen/wiki/Faster
         ; :jvm-opts ^:replace [;; important to allow logging to standard out
         ;                      "-Des.foreground=true"
         ;                      ;; Use the following to enable JMX profiling with visualvm
         ;                      "-Dcom.sun.management.jmxremote"
         ;                      "-Dcom.sun.management.jmxremote.ssl=false"
         ;                      "-Dcom.sun.management.jmxremote.authenticate=false"
         ;                      "-Dcom.sun.management.jmxremote.port=1098"]
         :source-paths ["src" "dev"]}
   :uberjar {:aot :all}}

  :aliases {;; Packages the spatial search plugin
            "package" ["do"
                       "clean,"
                       "uberjar,"
                       "shell" "zip" "-j" ~plugin-zip-name ~uberjar-name]

            ;; Packages and installs the plugin into the local elastic search vm
            "install-local" ["do"
                             "package,"
                             "shell" "../../cmr-vms/elastic_local/install_plugin.sh" ~plugin-zip-name "spatialsearch-plugin,"
                             "clean"]

            "install-aws" ["do"
                           "package,"
                           ;; IP address is hard coded for now
                           "shell" "../cmr-vms/elastic_aws/install_plugin.sh" "54.193.23.62" ~plugin-zip-name "spatialsearch-plugin"]
            "install-workload" ["do"
                                "package,"
                                "shell" "install_plugin_into_workload.sh" ~plugin-zip-name "spatialsearch-plugin"]
            })
