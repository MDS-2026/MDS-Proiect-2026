# MDS‑Proiect‑2026

Aplicație full‑stack: Spring Boot (backend) + React (Vite). Funcționalități principale: gestionare grupuri, portofele ierarhice, active, carduri virtuale și preview live pentru split‑ul de plată (vouchere / mile / cash).

---

## Scop
- Gestionare bugete și active pentru echipe.
- Vizualizare live a modului în care o plată este acoperită (voucher, mile, cash).
- Flux: autentificare → administrare grup → wallet → tranzacție → checkout preview.

## Structură proiect
- Backend: `src/main/java/...` (Spring Boot, Maven)  
- Frontend: `frontend/` (React + Vite)  
- Config & scripturi: `pom.xml`, `frontend/package.json`

## Cerințe
- Java 17+
- Maven 3.8+
- Node.js 18+ și npm / yarn

## Quick start (dezvoltare)
1. Pornire backend (din root proiect):
```bash
mvn clean install
mvn spring-boot:run
# sau (wrapper)
./mvnw clean install
./mvnw spring-boot:run
```
Backend: `http://localhost:8080`

2. Pornire frontend:
```bash
cd frontend
npm install
npm run dev
```
Vite dev server: `http://localhost:5173`

## Config & variabile recomandate
- Frontend (opțional): `frontend/.env`
```env
VITE_API_BASE=http://localhost:8080
```
- Cheie token în localStorage recomandată: `fairpay_token` (standardizează în tot frontend-ul)
- Backend: configurează `application.yml`/`application.properties` pentru DB, JWT secret și CORS

## Endpoint principal (checkout preview)
```
GET /api/checkout/preview?walletId={walletId}&amount={amount}
```
Răspuns tipic:
```json
{ "voucherAmount": 12.34, "milesAmount": 5.00, "cashAmount": 82.66 }
```
Exemplu curl:
```bash
curl -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/checkout/preview?walletId=abc&amount=100"
```

## Arhitectură & note
- Backend: REST controllers → services → repositories (JPA). DTO-uri pentru API.
- Frontend: React + react-router, componente reutilizabile (Modal, CheckoutPreview). Debounce pe input pentru preview.
- Logica split se rulează pe backend; frontend cere preview și afișează voucher/miles/cash.

## Troubleshooting rapid
- Vite: `Failed to resolve import './api'` — verifică sau adaugă `frontend/src/api.js` (client fetch minimal).
- CORS: activează `@CrossOrigin(origins = "http://localhost:5173")` sau configurează global.
- Preview gol/eroare: DevTools → Network — verifică request, query params, Authorization header, status, body JSON.
- Token inconsistențe: unifică cheile (`fairpay_token`, `token`, `authToken`) în frontend.
- Lombok (backend): asigură dependența în `pom.xml` și activează annotation processing în IDE sau convertește DTO-urile în POJO.

## Test & build
- Backend:
```bash
mvn test
mvn clean package
```
- Frontend:
```bash
cd frontend
npm test
npm run build
```

## Debug checklist (pentru bug reports)
Include:
- Pași de reproducere
- Request + response din DevTools Network (headers + body)
- Console errors din browser
- Backend logs / stacktrace

## Contribuire
Workflow recomandat: fork → branch → PR. Include descriere, pași de testare și exemple input/output.

## Licență
Adaugă fișier `LICENSE` dacă intenționezi publicare/distribuire.
EOF
``` 

