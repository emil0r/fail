(defproject fail "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.0"]

                 ;; crux
                 [juxt/crux-core "20.04-1.8.1-alpha"]
                 [juxt/crux-jdbc "20.04-1.8.1-alpha"]
                 [com.h2database/h2 "1.4.200"]

                 ;; time
                 [tick "0.4.23-alpha"]
                 
                 ;; filesystem
                 [me.raynes/fs "1.4.6"]

                 ;; logging
                 [com.taoensso/timbre "4.10.0"]
                 [com.fzakaria/slf4j-timbre "0.3.12"]

                 ;; structure
                 [com.stuartsierra/component "1.0.0"]]

  :repl-options {:init-ns fail.core})
