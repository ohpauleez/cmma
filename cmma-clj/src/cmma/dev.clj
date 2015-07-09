(ns cmma.dev
  (:require [clojure.walk :as walk]))

;; Simpler debug-repl :: https://github.com/stuarthalloway/circumspec/blob/master/src/clojure/contrib/debug.clj
(defmacro local-bindings
  "Produces a map of the names of local bindings to their values."
  []
  (let [symbols (map key @clojure.lang.Compiler/LOCAL_ENV)]
    (zipmap (map (fn [sym] `(quote ~sym)) symbols) symbols)))

(def ^:dynamic *locals*)
(defn eval-with-locals
  "Evals a form with given locals.
  The locals should be a map of symbols to values."
  [locals form]
  (binding [*locals* locals]
    (eval
      `(let ~(vec (mapcat #(list % `(*locals* '~%)) (keys locals)))
         ~form))))

(defn dr-read
  [request-prompt request-exit]
  (let [input (clojure.main/repl-read request-prompt request-exit)]
    (if (#{'() request-exit request-prompt} input)
      request-exit
      input)))

(def ^{:dynamic true :tag 'long} level (long 0))
(def counter (atom 1000))
(defn inc-counter []
  (swap! counter inc))

(def element (atom nil))

(def quit-dr-exception
  (proxy [Exception java.util.Enumeration] []
    (nextElement [] @element)))

(defn quit-dr [& form]
  (reset! element (first form))
  (throw quit-dr-exception))

(defn caught [exc]
  (if (or (= exc quit-dr-exception)
          (= (.getCause ^Throwable exc) quit-dr-exception))
    (throw quit-dr-exception)
    (clojure.main/repl-caught exc)))

(defmacro debug-repl
  "Starts a REPL with the local bindings available.
   * () - quit the repl
   * (quit-dr some-value) - quit the repl returning `some-value`; Useful when embedding DR
   * (in-debug-ns) - move to the ns where the repl was started
   * debug-meta - metadata captured at the repl call site"
  ([]
   `(debug-repl nil ~(meta &form)))
  ([retform]
   `(debug-repl ~retform ~(meta &form)))
  ([retform form-meta]
   `(debug-repl ~retform ~(meta &form) ~{}))
  ([retform form-meta injected-locals]
   `(let [debug-meta# ~(assoc form-meta
                              :ns *ns*
                              :file *file*)
          eval-fn# (fn [e-form#]
                     (eval-with-locals (merge {(symbol "debug-meta") debug-meta#
                                               (symbol "in-debug-ns") (fn [] (in-ns (ns-name (:ns debug-meta#))))}
                                              ~injected-locals
                                              (local-bindings)
                                              {(symbol "quit-dr") ~quit-dr})
                                       e-form#))]
      (try
        (binding [level (unchecked-inc level)]
          (clojure.main/repl
            :init (fn []
                    (use 'clojure.core)
                    (use 'clojure.repl))
            :prompt #(print (str "dr-" level "-" (inc-counter) " [" *ns* "]=> "))
            :eval eval-fn#
            :read dr-read
            :caught caught))
        (catch Exception e#
          (if (= e# quit-dr-exception)
            (if-let [new-form# (.nextElement ^java.util.Enumeration quit-dr-exception)]
              (eval-fn# new-form#)
              (eval-fn# ~retform))
            (throw e#)))))))

(defmacro assert-repl [assertion-form]
  `(when *assert*
     (if-not ~assertion-form
       (debug-repl)
       ;;Forward standard assert behavior, even if that's just pr-str
       (assert ~assertion-form))))

(defmacro try-repl [body-form]
  `(try
     ~body-form
     (catch Throwable ex#
       (let [~'debug-exception ex#]
         (debug-repl)))))

(defn make-error-handler
  "This takes a function that takes two args
  and returns an UncaughtExceptionHandler that will run that function with the
  thread/runnable and the Exception."
  [f]
  (proxy [Thread$UncaughtExceptionHandler] []
    (uncaughtException [thread exception]
      (f thread exception))))

(def debug-ex-handler (make-error-handler (fn [thread debug-exception] (debug-repl))))

(defn debug-uncaught-exceptions! []
  (Thread/setDefaultUncaughtExceptionHandler debug-ex-handler))

;; Easy searching for the function I want :: https://gist.github.com/alandipert/1619740
(defn findcore
  "Returns a lazy sequence of functions in clojure.core that, when applied to args,
  return ret."
  ([args ret]
   (findcore (filter #(not (:macro (meta %)))
                     (vals (ns-publics 'clojure.core))) args ret))
  ([[f & fns] args ret]
   (lazy-seq
     (when f
       (if (binding [*out* (proxy [java.io.Writer] []
                             (write [_])
                             (close [])
                             (flush []))]
             (try
               (= ret (apply f args))
               (catch Throwable t)))
         (cons (:name (meta f)) (findcore fns args ret))
         (findcore fns args ret))))))

(defmacro fc [args ret]
  `(findcore '~args '~ret))

;; Useful one-off functions

;; Construct
;; (construct [inc dec] [10 10]) => (11 9)
(def construct (partial map deliver))


(defn- formize-progress [prog-vec]
  (let [processed-form (loop [skip-elem 0
                              token (first prog-vec)
                              new-form []
                              progress (next prog-vec)]
                         (cond
                           (and (nil? progress)
                                (zero? skip-elem)) (conj new-form token)
                           (nil? progress) new-form
                           (and (zero? skip-elem)
                                (coll? token)) (recur (count token) (first progress) (conj new-form token) (next progress))
                           (pos? skip-elem) (recur (dec skip-elem) (first progress) new-form (next progress))
                           (zero? skip-elem) (recur skip-elem (first progress) (conj new-form token) (next progress))))]
    `(~@processed-form)))

(defn- print-form [form]
  (println "\nFinal form:" form)
  form)

(defn step-debug [form]
  "Step through an expression with a debug-repl.
  Empty list is next step/continue: ()
  Other locals:
   * form - the whole form you're stepping through
   * current-form - the current form/token you're on
   * step - what the current-step evals to
   * info - a string of all the step information
   * progress - an atom'd vector of all forms/tokens you've seen so far - CURRENTLY REMOVED
   * *locals* - a map of all locals in scope"
  (let [progress (atom [])]
    (eval
      (doto
        (walk/prewalk (fn [current-form]
                      (if (not= current-form form)
                        (do (swap! progress conj current-form)
                            (try
                              (let [step (eval current-form)
                                    info (str "\nform:" form
                                              "\ncurrent-form:" current-form
                                              "\nprogress:" (formize-progress @progress)
                                              "\nstep:" step)
                                    sdbg `(~@(formize-progress @progress) (debug-repl ~current-form
                                                                                      {}
                                                                                      {(symbol "form") ~form
                                                                                          (symbol "current-form") ~current-form
                                                                                          ;(symbol "progress") ~progress
                                                                                          (symbol "step") ~step
                                                                                          (symbol "info") ~info}))]
                                (println info)
                                (eval sdbg))
                              (catch Throwable t
                                current-form)))
                        current-form))
                    form)
        print-form))))

