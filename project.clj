(defproject com.semperos/otto "0.1.0-SNAPSHOT"
  :description "Domain modeling with a declarative serialization story"
  :url "https://github.com/semperos/otto"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :global-vars {*warn-on-reflection* true}
  :dependencies [[prismatic/schema "1.0.3"]]
  :deploy-repositories [["releases" {:url "https://clojars.org/repo"
                                     :creds :gpg}]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.7.0"]
                                  [cheshire "5.5.0"]
                                  [org.clojure/test.check "0.8.2"]
                                  [com.gfredericks/test.chuck "0.2.0"]]}})
