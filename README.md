
# CityTrip Connect

Wij ontwikkelen **een mobiele app** waarmee gebruikers tijdens een citytrip interessante **horecazaken, hotels, bezienswaardigheden en andere locaties** kunnen ontdekken, toevoegen en delen met anderen.
Ons doel is om een platform te creëren waar reizigers elkaar kunnen inspireren door hun favoriete plekken te delen — een sociale citytrip-ervaring die zowel praktisch als leuk is.

---

## Functionele minimumvereisten

Onze app zal minimaal volgende functies bevatten:

* **Authenticatie & registratie** via **Firebase Authentication**
* **Gecentraliseerde dataopslag** met behulp van **Firebase**
* **Onbeperkte toevoeging van steden**
* **Locaties opslaan** op basis van je huidige positie, inclusief **foto** en **categorie**
* **Gebruikersinteractie:** anderen kunnen locaties **beoordelen (rating)** en **commentaar geven**
* **Filterfunctionaliteit** op basis van locatiecategorieën
* Na het filteren wordt zowel een **lijstweergave** als een **kaartweergave (OSM – OpenStreetMap)** getoond

> ⚠️ Zonder aanwezigheid van deze functionele minimumvereisten wordt het project niet beoordeeld.

---

## Nice-to-haves

Om de gebruikerservaring te verbeteren, willen we extra functionaliteiten toevoegen zoals:

* Het **berekenen van de afstand** tot een locatie wanneer je erop klikt (in de lijst of op de kaart)
* **Caching van locatiedata:** bij het opstarten wordt alle data uit Firebase ingeladen en lokaal opgeslagen in een **Room database**, met een **refresh-knop** om updates op te halen
* De mogelijkheid om **berichten te sturen** naar andere gebruikers

---

## Indicatieve puntenverdeling

| Onderdeel                                    | Punten |
| -------------------------------------------- | ------ |
| Authenticatie & Firebase                     | **3**  |
| Inbrengen gegevens, weergave & gebruiksgemak | **3**  |
| Filteren & lijstweergave                     | **3**  |
| Kaartfunctionaliteit (OSM)                   | **4**  |
| Nice-to-haves                                | **7**  |
| Bugs                                         | **–1** |
| Crash                                        | **–3** |

