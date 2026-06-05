# MDS-Proiect-2026

Aplicație full‑stack: Spring Boot (backend) + React (Vite) — gestionare grupuri, portofele, active, carduri virtuale și preview live pentru split de plată.

## Structură
- backend: `src/main/java/...` (Spring Boot)
- frontend: `frontend/` (React + Vite)

## Cerințe
- Java 17+, Maven 3.8+, Node.js 18+

## Pornire rapidă
1. Backend:
   ```
   mvn clean install
   mvn spring-boot:run
   ```
2. Frontend:
   ```
   cd frontend
   npm install
   npm run dev
   ```

## Config
- (optional) `frontend/.env`: `VITE_API_BASE=http://localhost:8080`
- Token localStorage recomandat: `fairpay_token`

## Endpoint util
- `GET /api/checkout/preview?walletId={id}&amount={amount}`  
  răspuns: `{ "voucherAmount": n, "milesAmount": n, "cashAmount": n }`

## Probleme frecvente
- Vite: import "./api" lipsă → adaugă `frontend/src/api.js` (client fetch minimal).
- CORS → activează `@CrossOrigin(origins = "http://localhost:5173")` pe backend.
- Preview gol → verifică DevTools Network (request/response) și token în localStorage.

## Test & build
- Backend: `mvn test` / `mvn clean package`
- Frontend: `cd frontend && npm test` / `npm run build`
