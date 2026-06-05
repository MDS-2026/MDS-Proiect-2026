# MDS‑Proiect‑2026

Aplicație full‑stack: Spring Boot (backend) + React (Vite). Funcționalități: grupuri, portofele, active, carduri virtuale și preview live pentru split de plată.

## Structură
- backend: `src/main/java/...` (Spring Boot, Maven)  
- frontend: `frontend/` (React + Vite)

## Cerințe
- Java 17+, Maven 3.8+, Node.js 18+ și npm / yarn

## Pornire rapidă
1. Backend (rădăcina proiectului)
```bash
mvn clean install
mvn spring-boot:run
# sau (wrapper)
./mvnw clean install
./mvnw spring-boot:run
```
Server: `http://localhost:8080`

2. Frontend
```bash
cd frontend
npm install
npm run dev
```
Vite: `http://localhost:5173`

## Config recomandată
- `frontend/.env` (opțional):
```env
VITE_API_BASE=http://localhost:8080
```
- Cheie token localStorage recomandată: `fairpay_token`

## Endpoint util
- Checkout preview:
```
GET /api/checkout/preview?walletId={id}&amount={amount}
```
Răspuns tipic:
```json
{ "voucherAmount": 12.34, "milesAmount": 5.00, "cashAmount": 82.66 }
```

## Probleme comune & remedii rapide
- Vite: `Failed to resolve import './api'` — adaugă `frontend/src/api.js` (client fetch minimal).
- CORS: activează `@CrossOrigin(origins = "http://localhost:5173")` sau configurează global pe backend.
- Preview gol: verifică DevTools → Network (request, query params, Authorization header, status, body).
- Token inconsistențe: unifică cheile (`fairpay_token`, `token`, `authToken`).
- Lombok (backend): asigură dependenţa în `pom.xml` și activează annotation processing în IDE.

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

## Debug / Contribuire
Include pași de reproducere, request/response din DevTools, console errors și logs backend în issue. Contribuții: fork → branch → PR cu descriere și pași de testare.

## Licență
Adaugă `LICENSE` dacă intenționezi distribuirea publică.