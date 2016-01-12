(ns todomvc.core
  (:require [goog.dom :as dom]
            [inkle.core :as inkle]))

(defonce todos (inkle/iatom (sorted-map)))

(defonce counter (inkle/iatom 0))

(defn add-todo [text]
  (let [id (swap! counter inc)]
    (swap! todos assoc id {:id id :title text :done false})))

(defn toggle [id] (swap! todos update-in [id :done] not))
(defn save [id title]
  (swap! todos assoc-in [id :title] title))
(defn delete [id] (swap! todos dissoc id))

(defn mmap [m f a] (->> m (f a) (into (empty m))))
(defn complete-all [v] (swap! todos mmap map #(assoc-in % [1 :done] v)))
(defn clear-done [] (swap! todos mmap remove #(get-in % [1 :done])))

(defonce init (do
                (add-todo "First job of the day")
                (add-todo "Another thing I've gotta do")
                (add-todo "More jobs!")
                (add-todo "One last thing and I'm done!")
                (complete-all true)))


(defn todo-input [{:keys [class title on-save on-stop]}]
  (let [val (inkle/iatom title) 
        stop #(do (reset! val "")
                  (if on-stop (on-stop)))
        save #(let [v (-> @val str)]
                (if-not (empty? v) (on-save v))
                (stop))]
    (fn [props]
      [:input (merge props
                     {:class class :type "text" :value @val :on-blur save
                      :oninput #(reset! val (-> % .-target .-value))
                      :onkeydown #(case (.-which %)
                                      13 (save)
                                      27 (stop)
                                      nil)})])))

(defn todo-stats [{:keys [filt active done]}]
  (let [props-for (fn [name]
                    {:class (if (= name @filt) "selected")
                     :onclick #(reset! filt name)})]
    [:div
     [:span#todo-count
      [:strong active] " " (case active 1 "item" "items") " left"]
     [:ul#filters
      [:li [:a (props-for :all) "All"]]
      [:li [:a (props-for :active) "Active"]]
      [:li [:a (props-for :done) "Completed"]]]
     (when (pos? done)
       [:button#clear-completed {:onclick clear-done}
        "Clear completed " done])]))


(defn todo-item [& args]
  (let [editing (inkle/iatom false)]
    (fn [{:keys [id done title]}]
      [:li {:class (str (if done "completed ")
                        (if @editing "editing"))}
       [:div.view
        [:input.toggle {:type "checkbox" :checked done
                        :onchange #(toggle id)}]
        [:label {:ondblclick #(reset! editing true)} title]
        [:button.destroy {:onclick #(delete id)}]]
       (when @editing
         [todo-input {} {:class "edit"
                         :title title
                         :on-save #(save id %)
                         :on-stop #(reset! editing false)}])])))


(defn todo-app [props]
  (let [filt (inkle/iatom :all)]
    (fn []
      (let [items (vals @todos)
            done (->> items (filter :done) count)
            active (- (count items) done)]
        [:div
         [:section#todoapp
          [:header#header
           [:h1 "todos"]
           [todo-input {} {:id "new-todo"
                           :placeholder "What needs to be done?"
                           :on-save add-todo}]]
          (when (-> items count pos?)
            [:div
             [:section#main
              [:input#toggle-all {:type "checkbox" :checked (zero? active)
                                  :onchange #(complete-all (pos? active))}]
              [:label {:for "toggle-all"} "Mark all as complete"]
              [:ul#todo-list
               (for [todo (filter (case @filt
                                    :active (complement :done)
                                    :done :done
                                    :all identity) items)]
                 [todo-item {} todo])]]
             [:footer#footer
              [todo-stats {} {:active active :done done :filt filt}]]])]
         [:footer#info
          [:p "Double-click to edit a todo"]]]))))

(inkle/renderer (dom/getElement "app") [todo-app])

