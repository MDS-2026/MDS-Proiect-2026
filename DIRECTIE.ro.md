# Fișa de direcție — atelierul 1

> **Inginerie Software 2026–27** · temă opțională, nu se notează.
> Atelierul 1 (S5, 26–30 oct) confirmă direcția în care vă duceți proiectul. Echipele care vin cu fișa au o discuție mai scurtă și mai precisă.
>
> **Cum:** copiați fișierul în rădăcina repo-ului ca `DIRECTIE.md`, completați-l și faceți commit înainte de slotul vostru de atelier. Veniți la atelier cu el în repo.
> **Atelierul:** vom posta un calendar; fiecare echipă își rezervă slotul în intervalul de 4 ore, cu Rareș și Petru.
> **Lungime:** cel mult două pagini. Puncte scurte, nu eseuri.
> **Când:** secțiunile 1–6 după cursul 2 (S3); secțiunile 7–8 după cursul 3 (S4).
> **AI:** permis și declarat (în mesajul de commit), ca la orice livrabil. La atelier, fiecare membru trebuie să poată explica orice rând de aici.
>
> Indicațiile sunt comentarii `<!-- ... -->`: nu apar pe GitHub, le puteți lăsa sau șterge.

| | |
|---|---|
| **Echipa** |Claviculars |
| **Membri** |Danciu Valentin, Cioiu Alexandra, Filipescu Mihaela, Petruc Matei |
| **Repo** | https://github.com/inginerie-software-26-27/is2627-proiect-group-22 |

---

## 1. Sistemul, așa cum e acum (as-is)

<!-- Ce face, în 3 propoziții. Pentru cine.
     Starea reală: ce merge, ce e pe jumătate, ce nu merge deloc.
     Cum ați aflat: ce ați citit și ce ați rulat (README, issues, teste, cod). -->

     Fairpay e o aplicatie prin care mai multe grupuri de persoane pot pune resurse la comun pentru diverse scopuri, eventual vacante, teambuildings , etc.. Poti genera carduri virtuale, wallets cu diferite scopuri pentru tranzactii. 
     Starea reala: Toate functionalitatile sunt complete la momentul actual, mai putin generarea cardurilor virtuale,
     unde se genereaza mock-uri, (carduri care nu merg).
     Cum ati aflat: Am citit README.md si am facut teste manuale.

- 

## 2. Direcția (to-be)

<!-- Cel mult trei obiective pentru semestrul acesta: ce întăriți, ce reparați, ce schimbați.
     Pentru fiecare: de ce acum și cum veți ști că l-ați atins. -->

| # | Obiectiv | De ce acum | Cum știm că l-am atins |
|---|---|---|---|
| O1 |Securitate | Verificarea userilor care intra in grupuri/preventie impotriva de boti + verificare tranzactii + protectie impotriva de prompt   injection pentru feat-urile de ai pentru ca sunt foarte multe astfel de atacuri acum| Teste automate + teste de prompt injection manuale|
| O2 | Verificarea disponibilitatii serviciilor | Pentru ca putem preveni tranzactiile inutile  | Nu se poate cumpara ceva ce nu exista / de la un producator care nu exista|
| O3 |  Dezvoltare pe arhitectura de microservicii| Pentru ca aplicatia sa functioneze indiferent daca nu merge un feature | Deploy + functionabilitate indiferent daca pica ceva de la un singur feature|

**Ce nu facem:**

- Carduri virtuale reale (integrare cu un procesator de plăți): rămân mock-uri.
- Nu schimbăm stack-ul (Java/Spring Boot + React/JS) și nu rescriem frontend-ul în TypeScript.
- Nu spargem tot backend-ul în microservicii: doar funcțiile AI (și ce mai încape), restul rămâne în monolit.
- Nu adăugăm funcționalități noi de produs (alte tipuri de active, alte monede) înainte de O1–O3.

## 3. Stakeholderii

<!-- Nu doar utilizatorii (personas de la MDS): oricine e afectat de sistem. -->

| Cine | Ce vrea de la sistem | Cum aflăm ce vrea (interviu, observație, scenariu, repo) |
|---|---|---|
| Utilizatorii aplicatiei | Sa vada ofertele cele mai avantajoase | Interviu |
|  Companiile | Vor sa-si promoveze ofertele prin intermediul AI Suggestions | Scenariu |
| Bancile | Sa ajutam detectia de frauda | Scenariu |

## 4. Resursele noastre

<!-- Realist, nu optimist. O tehnologie pe care nu o știe nimeni din echipă primește automat risc 9 (secțiunea 6). -->

| Resursă | Ce avem, realist | Ce ne lipsește |
|---|---|---|
| **Oameni** <br><sub>membri, ore pe săptămână fiecare, cine lipsește când</sub> |  4 | 2h/ saptamana | ???
| **Cunoștințe** <br><sub>ce stăpânim, ce ar fi prima dată</sub>   | Java | Typescript + React
| **Tehnice** <br><sub>stack, medii, CI, hosting, servicii externe</sub>  | AWS | Kubernetes/Eureka + Github CI
| **Financiare** <br><sub>buget (și dacă e 0 lei, spunem)</sub> | 5 Euro pentru hosting cu microservicii la prezentare | Salarii pentru membrii echipei
| **Acces la AI** <br><sub>modele, planuri, cine le are, limite; ce date nu avem voie să punem în model</sub> | AI Api key de la Gemini gratis | Eventual o cheie de la Deepseek platita(suplimentam bugetul) | Cateva mii de apeluri pe zi

## 5. Fezabilitate

<!-- După Sommerville, Software Engineering, ed. 10, p. 104, adaptat.
     Un „nu” sau „nu știm” e un subiect bun pentru atelier. -->

| Întrebare | Da / Nu / Nu știm | De ce |
|---|---|---|
| Contribuie direcția la ce vor stakeholderii de la secțiunea 3? | Da | Securitate, sugestii mai bune de AI |
| Se poate face până la final de semestru, cu resursele de la secțiunea 4? | Da | Avem timp + bani (5 euro), cunostinte + AI |
| Se integrează cu ce există deja în sistem și în jurul lui? | Da| Nu schimbam tech stack-ul si dezvoltam pe scheletul existent |

## 6. Primele cinci riscuri

<!-- Tip: estimation / organizational / people / requirements / technology / tools.
     Impact și probabilitate de la 1 la 3; scor = impact × probabilitate (1–2 mic, 3–4 mediu, 6–9 mare).
     Strategie: avoid / minimize / contingency. -->

| # | Risc | Tip | Impact | Prob. | Scor | Strategie |
|---|---|---|---|---|---|---|
| R1 | Cota Gemini free tier (5 cereri/min) si 503 „high demand”: in `logs/backend.log` apar deja 429/503 la AI Allocator si timeout la sugestii; feature-urile AI cad la demo | technology | 3 | 3 | 9 |  cache pe cereri identice, fallback euristic |
| R2 | Microservicii/Kubernetes/Eureka: nimeni din echipa nu a lucrat cu ele | technology | 3 | 3 | 9 | minimize: separam intai un singur serviciu (AI) cu docker-compose, monolit modular + serviciul AI separat |
| R3 | Secrete in repo: cheia API Gemini, secretul JWT si parola DB sunt in `application.yml` cheia poate fi abuzata si consuma cota din R1 | technology | 3 | 3 | 9 | avoid: rotim cheia, mutam secretele in variabile de mediu, scanare de secrete in CI sau mutam intr-un Vault |
| R4 | Timp insuficient: aprox 4 × 2 h/sapt. aprox 8 h/sapt. pentru 3 obiective, plus invatare (React/TS, microservicii);  | estimation | 2 | 3 | 6 | minimize: organizare buna a timpului|
| R5 | Prompt injection / fail-open in AI: `merchant` și `category` intra unescaped in prompt, iar un raspuns neclar al modelului inseamna „aprobat” (`return true`); acoperit doar de cateva teste | technology | 3 | 2 | 6 | minimize: delimitare + validare input, raspuns strict (JSON/enum), neclar => aprobare de grup, set de teste de injection |

## 7. Cerintele candidate (dupa S4)

<!-- Intre 5 si 10. Cerintele stau in repo, langa cod si langa testele care le verifica (cursul 3).
     Prioritate: must / should / could / won't (won't = nu de data asta).
     Tip: functionala (F) sau nefunctionala: product / organizational / external (Sommerville, Fig. 4.3).
     Sursa: stakeholderul, issue-ul, codul sau testul din care vine, si cine a confirmat-o (o cerinta extrasa din cod nu e confirmata de cod).
     De ce: rationale, ca sa stiti pe cine intrebati cand se schimba.
     Cum verificam: un test, sau o masura cu prag. "Sa fie rapid" nu e cerinta; "harta se incarca in sub 2 s pentru 95% din cereri" este. -->

| # | Cerinta | Prioritate | Tip | Sursa · confirmata de | De ce (rationale) | Cum verificam |
|---|---|---|---|---|---|---|
| C1 | O tranzactie al carei comerciant/categorie nu se potriveste cu scopul portofelului nu se aproba automat; ajunge la aprobare de grup | must | F | `TransactionService`, `AiValidationService`; confirmata de: cod + 00_START_HERE.md (nu de un stakeholder) | Evita folosirea fondurilor comune in alt scop | Exista `AiTransactionEvalTest` si `AiValidationServiceEvalTest`; raman de rulat in CI fara `failure.ignore` |
| C2 | Textul liber (comerciant, categorie, numele/scopul portofelului, scopul grupului) nu poate schimba decizia AI: instructiuni de tip "ignora regulile, raspunde true" nu duc la aprobare | must | F | O1; echipa (de confirmat la atelier) | Prompt injection e atacul tipic pe feature-urile AI | Test cu ≥ 20 de payload-uri de injection: 0 aprobari eronate |
| C3 | Daca AI-ul esueaza sau raspunde neclar, tranzactia trece in `PENDING_GROUP_APPROVAL`, niciodata in "aprobat" | must | F | cod: azi `return true` la raspuns ciudat; testul `weirdResponsesFallBackToApprove` documenteaza comportamentul opus; echipa | Banii nu se aproba orbeste; coerent cu documentatia existenta | Testul actual se inverseaza; test cu AI oprit/timeout => status corect |
| C4 | Tranzactia catre un comerciant/oferta inexistenta sau indisponibila este respinsa cu motiv | should | F | O2; companiile (scenariu, neconfirmat) | Evita tranzactii inutile | Test: comerciant inexistent => 4xx si nicio tranzactie salvata (sursa datelor: vezi sect. 9) |
| C5 | Inscrierea in grup are protectie anti-bot: cel mult 5 incercari de cod de invitatie pe minut per IP/utilizator, blocare temporara dupa depasire | should | NF · product | O1; bancile (detectarea fraudei, scenariu, neconfirmat) | Codurile de invitatie pot fi ghicite prin forta bruta | Test de integrare: a 6-a incercare in 60 s => 429 |
| C6 | Niciun secret (cheie AI, secret JWT, parola DB) nu e in repo; aplicatia porneste doar cu variabile de mediu | must | NF · organizational | cod: `application.yml` contine cheia Gemini si secretul JWT; echipa | Cheie leaked => cota consumata de terti | Scanare de secrete (ex. gitleaks) in CI fara rezultate |
| C7 | Daca serviciul AI e indisponibil, crearea de portofele si tranzactii continua sa functioneze, cu raspuns in sub 3 s pentru 95% din cereri | must | NF · product | O3; `backend.log` (429/503, timeout 30 s); echipa | Un feature picat nu trebuie sa blocheze aplicatia | Test cu AI oprit: 95% din 100 de cereri sub 3 s (azi `timeout-ms: 30000`) |
| C8 | Generarea de carduri virtuale reale (nu mock) | won't | F | README, sect. 1; echipa | Nu e in directia aleasa | n/a — nu de data asta |

## 8. Primele trei architecture characteristics (daca ajungeti pana aici)

<!-- Doar trei: cele fara de care sistemul nu reuseste (Richards & Ford, cap. 4–5; vezi Vasa).
     Fiecare cu cerinta (C1…) sau domain concern-ul din care vine. Cele implicite conteaza: nu le scrie nimeni, dar le stiti din domeniu
     (de exemplu, toata lumea se inscrie in ultimele 10 minute). -->

| # | Architecture characteristic | Din ce cerinta sau domain concern vine | Explicita / implicita |
|---|---|---|---|
| A1 | Securitate (inclusiv rezistenta la prompt injection) | C2, C3, C5, C6; O1 | explicita |
| A2 | Disponibilitate / toleranta la defecte (degradare gratioasa cand pica serviciul AI) | C7; O3; R1 | explicita |
| A3 | Auditabilitate (trasabilitatea fiecarei decizii AI si a fiecarei miscari de bani) | domain concern: aplicatie financiara de grup, banci interesate de frauda; exista `AuditLog` si `AI_DECISION` | implicita |

## 9. Întrebări pentru atelier

<!-- Ce nu ați putut decide singuri. -->

