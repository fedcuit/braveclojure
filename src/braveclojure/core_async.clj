(ns braveclojure.core-async
  (:require [clojure.core.async
             :as a
             :refer [>! <! >!! <!! go chan buffer close! thread alts! alts!! timeout]]
            [clojure.string :as str])
  (:import java.util.UUID)
  (:gen-class))

(defn -main
  [& args]
  (println "Hello, World!"))

(comment
  ;;; processes
  (def echo-chan (chan))
  (go (println (<! echo-chan)))
  (>!! echo-chan "ketchup")

  ;;; buffering
  (def echo-buffer (chan 2))
  (>!! echo-buffer "ketchup")
  (>!! echo-buffer "ketchup")

  ;; it will block your REPL
  (>!! echo-buffer "ketchup")

  ;; blocking and parking
  (def hi-chan (chan))
  (doseq [n (range 1000)]
    (go (>! hi-chan (str "hi " n))))

  ;; thread
  (thread (println (<!! echo-chan)))
  (>!! echo-chan "mustard")

  (let [t (thread "chili")]
    (<!! t))

  ;; hot dog machine
  (defn hot-dog-machine
    []
    (let [in (chan)
          out (chan)]
      (go (<! in)
          (>! out "hot dog"))
      [in out]))

  ;; time for a hot dog
  (let [[in out] (hot-dog-machine)]
    (>!! in "pocket lint")
    (<!! out))

  ;; hot dog machine v2
  (defn hot-dog-machine-v2
    [hot-dog-count]
    (let [in (chan)
          out (chan)]
      (go (loop [hc hot-dog-count]
            (if (> hc 0)
              (let [input (<! in)]
                (if (= 3 input)
                  (do
                    (>! out "hot dog")
                    (recur (dec hc)))
                  (do
                    (>! out "wilted lettuce")
                    (recur hc))))
              (do (close! in)
                  (close! out)))))
      [in out]))

  ;; interacting with a robust hot dog vending machine process
  (let [[in out] (hot-dog-machine-v2 2)]
    (>!! in "pocket lint")
    (prn (str "From vending machine: " (<!! out)))

    (>!! in 3)
    (prn (str "From vending machine: " (<!! out)))

    (>!! in 3)
    (prn (str "From vending machine: " (<!! out)))

    (>!! in 3)
    (prn (str "From vending machine: " (<!! out))))

  ;; pipeline of processes
  (let [c1 (chan)
        c2 (chan)
        c3 (chan)]
    (go (>! c2 (str/upper-case (<! c1))))
    (go (>! c3 (str/reverse (<! c2))))
    (go (prn (<! c3)))
    (>!! c1 "redrum"))

  ;; alts!! core.async version of Promise.any()
  (defn upload
    [headshot c]
    (go (Thread/sleep (rand 100))
        (>! c headshot)))

  (let [c1 (chan)
        c2 (chan)
        c3 (chan)]
    (upload "serious.jpg" c1)
    (upload "fun.jpg" c2)
    (upload "sassy.jpg" c3)

    (let [[headshot channel] (alts!! [c1 c2 c3])]
      (prn "Sending headshot notification for " headshot)))

  ;; timeout channel (cannot take things out of it until it's closed)
  (let [c1 (chan)]
    (upload "serious.jpg" c1)
    (let [[headshot channel] (alts!! [c1 (timeout 20)])]
      (if headshot
        (prn "Sending headshot notification for" headshot)
        (prn "Time out!"))))

  ;; alts for put operations
  (let [c1 (chan)
        c2 (chan)]
    (go (<! c2))
    (let [[value channel] (alts!! [c1 [c2 "put something"]])]
      (prn value)
      (= channel c2)))


  ;;; queues


  (defn append-to-file
    [filename s]
    (prn "write to file")
    (spit filename s :append true))

  (defn format-quote
    [quote]
    (str "=== BEGIN QUOTE ===\n" quote "=== END QUOTE ===\n\n"))

  (defn random-quote
    []
    (format-quote (UUID/randomUUID)))

  (defn snag-quotes
    [filename num-quotes]
    (let [c (chan)]
      (go (while true (append-to-file filename (<! c))))
      (dotimes [n num-quotes] (thread (>!! c (random-quote))))))

  (snag-quotes "quotes" 2)

  ;; more process pipeline
  (defn upper-caser
    [in]
    (let [out (chan)]
      (go (while true (>! out (str/upper-case (<! in)))))
      out))

  (defn reverser
    [in]
    (let [out (chan)]
      (go (while true (>! out (str/reverse (<! in)))))
      out))

  (defn printer
    [in]
    (go (while true (prn (<! in)))))

  (def in-chan (chan))
  (def upper-caser-out (upper-caser in-chan))
  (def reverser-out (reverser upper-caser-out))
  (printer reverser-out)

  (>!! in-chan "redrum")
  (>!! in-chan "repaid"))