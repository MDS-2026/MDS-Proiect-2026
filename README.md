# MDS‑Proiect‑2026

Aplicație full‑stack: Spring Boot (backend) + React (Vite). Funcționalități: grupuri, portofele, active, carduri virtuale și preview live pentru split de plată.

## Structură
- backend: `src/main/java/...` (Spring Boot, Maven)  
- frontend: `frontend/` (React + Vite)

## Cerințe
- Java 17+, Maven 3.8+, Node.js 18+ și npm/yarn

## Pornire rapidă
1. Backend (rădăcina proiectului)
```bash
mvn clean install
mvn spring-boot:run
```
2. Frontend
```bash
cd frontend
npm install
npm run dev
```

## Config
- (opțional) `frontend/.env`:
```
VITE_API_BASE=http://localhost:8080
```
- Cheie token recomandată în localStorage: `fairpay_token`

## Endpoint util
- Checkout preview:
```
GET /api/checkout/preview?walletId={id}&amount={amount}
```
Răspuns tipic:
```json
{ "voucherAmount": 12.34, "milesAmount": 5.00, "cashAmount": 82.66 }
```

## Troubleshooting rapid
- Vite: "Failed to resolve import './api'": adaugă `frontend/src/api.js` (client fetch minimal).
- CORS: activează `@CrossOrigin(origins = "http://localhost:5173")` sau configurare globală.
- Preview gol: verifică DevTools → Network (request, Authorization header, status, body) și token în localStorage.
- Lombok (backend): asigură dependenţa în `pom.xml` și annotation processing în IDE.

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

## Contact / Debug
Pentru bug: trimite pași de reproducere, request/response din DevTools și log backend (stacktrace) pentru diagnostic.