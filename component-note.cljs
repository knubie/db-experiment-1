
[:page/name
 {:note [:note/name :note/content]}
 {:deck [:deck/name {:deck/cards [:card/content]}]}]

(defc app
  (case page/name
    :show-note
    {note [show-note]}

    :show-deck
    {deck [show-deck]}))


(defc show-note
  [:div
   [:h1 note/name]
   [:p note/content]])
  
(defc show-deck
  [:div
   [:h1 deck/name]
   [:ui {deck/cards [card]}]])

(defc card
  [:div
   [:p card/content]])
