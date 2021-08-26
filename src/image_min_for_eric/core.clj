(ns image-min-for-eric.core
  (:require [clojure.java.io :as io]
            [seesaw.core :as ss])
  ;(:import 'javafx.geometry.Pos)
  (:gen-class))

(def main-font {:font "Helvetica"
                :size 20
                :color "White"})

(defn TodoItem [{:keys [done? idx text]}]
  (ss/border-panel
    :padding (ss/insets
               :top 10
               :bottom 10
               :left 0
               :right 0)
    :left (ss/checkbox
            :font main-font
            :text text
            :selected done?
            :on-action {:event :swap-status :idx idx})
    :right (ss/button :text "X"
                      :on-action {:event :delete-item :idx idx})))

(defn MainWindow [{:keys [todos]}]
  (ss/box-panel
    :style "-fx-base: rgb(30, 30, 35);"
    :padding nil                                       ;(ss/insets :top-right-bottom-left 25)
    :children [(ss/box-panel
                 ;:alignment (javafx.geometry.Pos/CENTER)
                 :children [(ss/button :text "Select Directory"
                                       :font main-font
                                       :on-action {:event :pick-folder
                                                   :fn-fx/include {::input #{:text}}})

                            (ss/button :text "Process Images"
                                       :font main-font
                                       :on-action {:event :process-images
                                                   :fn-fx/include {::input #{:text}}})])

               (ss/v-bo
                 :children (map-indexed
                             (fn [idx todo]
                               (todo-item (assoc todo :idx idx)))
                             todos))]))

(defui Stage
       (render [this args]
               ))

(defmulti handle-event (fn [state event]
                          (:event event)))

(defmethod handle-event :height-change
  [state {:keys [idx]}]
  (update-in state [:todos idx :done?] (fn [x]
                                         (not x))))

(defmethod handle-event :swap-status
  [state {:keys [idx]}]
  (update-in state [:todos idx :done?] (fn [x]
                                         (not x))))
(defn item [i]
  {:done? false :text i})

(defmethod handle-event :pick-folder
  [state {:keys [fn-fx/includes]}]
  (let [window (.getWindow (.getScene (:target (:fn-fx/event includes))))
        dialog (doto (javafx.stage.FileChooser.) (.setTitle "Pick Directory"))
        file (utils/run-and-wait (.showOpenDialog dialog window))
        ;        folder-name (get-in includes [::input :text])
        folder (.list (io/file file))]
    (update-in state [:todos] into (map item folder))))

(defmethod handle-event :add-item
  [state {:keys [fn-fx/includes]}]
  (update-in state [:todos] conj {:done? false
                                  :text (get-in includes [::input :text])}))

(defmethod handle-event :delete-item
  [state {:keys [idx] :as asdf}]
  (update-in state [:todos] (fn [itms]
                              (vec (concat (take idx itms)
                                           (drop (inc idx) itms))))))

(defmethod handle-event :default
  [state event]
  (println "No handler for event " (:type event) event)
  state)

(defn -main []
  (let [;; Data State holds the business logic of our app
        data-state (atom {:todos [{:done? false
                                   :text  "Test"}]})

        ;; handler-fn handles events from the ui and updates the data state
        handler-fn (fn [event]
                     (try
                       (println event)
                       (swap! data-state handle-event event)
                       (catch Throwable ex
                         (println ex))))

        ;; ui-state holds the most recent state of the ui
        ui-state   (agent (dom/app (stage @data-state) handler-fn))]

    ;; Every time the data-state changes, queue up an update of the UI
    (add-watch data-state :ui (fn [_ _ _ _]
                                (send ui-state
                                      (fn [old-ui]
                                        (try
                                          (dom/update-app old-ui (stage @data-state))
                                          (catch Throwable ex
                                            (println ex)))))))))


(defn main []
  (ss/native!)
  (let [data-state (atom {:todos [{:done? false
                                   :text  "Test"}]})]
    (ss/invoke-later
      (-> (ss/frame :title "Image Converter for Éric ♡",
                    :content (ss/window
                              :title "Image Converter for Éric ♡",
                              :min-height 600,
                              :listen/height {:event :height-change
                                              :fn-fx/include {::input #{:text}}},
                              :shown true,
                              :scene (main-window args))
                    :on-close :exit)
          ss/pack!
          ss/show!))))
