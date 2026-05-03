# Testing Guide - AI Transaction Validator

## Prerequisites

```bash
# 1. Start PostgreSQL
docker-compose up -d postgres
docker-compose ps

# 2. Build application
./mvnw clean package -DskipTests

# 3. Set AI configuration (or leave empty for heuristic mode)
export AI_ENDPOINT="https://your-ai-service.com/validate"
export AI_API_KEY="sk-your-api-key"

# 4. Start backend
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

---

## Workflow: Register, Create Group, Create Wallet, Add Transactions

### 1. Register User

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePassword123"
  }'

# Response:
# {
#   "token": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
#   "email": "user@example.com"
# }

export TOKEN="<token_from_response>"
```

### 2. Create Group

```bash
curl -X POST http://localhost:8080/api/groups \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Travel Friends"
  }'

# Response:
# {
#   "id": "group-uuid-here",
#   "name": "Travel Friends",
#   "inviteCode": "ABCD1234",
#   ...
# }

export GROUP_ID="<group_id_from_response>"
```

### 3. Create Wallet

```bash
curl -X POST http://localhost:8080/api/groups/$GROUP_ID/wallets \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Flight Budget",
    "purpose": "International travel expenses",
    "budgetLimit": 5000.0,
    "autoApproveThreshold": 200.0
  }'

# Response:
# {
#   "id": "wallet-uuid-here",
#   "name": "Flight Budget",
#   "purpose": "International travel expenses",
#   "budgetLimit": 5000.0,
#   "autoApproveThreshold": 200.0,
#   ...
# }

export WALLET_ID="<wallet_id_from_response>"
```

---

## Test Scenarios

### ✅ Test 1: Valid Transaction (Should Auto-Approve)

```bash
curl -X POST http://localhost:8080/api/wallets/$WALLET_ID/transactions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 100.0,
    "merchant": "Air France",
    "category": "Flights"
  }'

# Expected Response:
# {
#   "id": "tx-uuid",
#   "amount": 100.0,
#   "merchant": "Air France",
#   "category": "Flights",
#   "status": "APPROVED",  ✅
#   "walletId": "...",
#   "walletName": "Flight Budget",
#   "createdAt": "2026-05-01T13:30:00Z"
# }
```

### ❌ Test 2: Invalid Transaction - Grocery on Flight Wallet (Should Decline)

```bash
curl -X POST http://localhost:8080/api/wallets/$WALLET_ID/transactions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 50.0,
    "merchant": "Lidl Supermarket",
    "category": "Grocery"
  }'

# Expected Response:
# {
#   "id": "tx-uuid-2",
#   "amount": 50.0,
#   "merchant": "Lidl Supermarket",
#   "category": "Grocery",
#   "status": "DECLINED",  ❌
#   "walletId": "...",
#   "walletName": "Flight Budget",
#   "createdAt": "2026-05-01T13:31:00Z"
# }

# Alert should have been sent to group members!
# Check logs for: "ALERT for group 'Travel Friends': Transaction Declined..."
```

### ❌ Test 3: Invalid Transaction - Electronics on Flight Wallet (Should Decline)

```bash
curl -X POST http://localhost:8080/api/wallets/$WALLET_ID/transactions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 300.0,
    "merchant": "Apple Store",
    "category": "Electronics"
  }'

# Expected Response: status = "DECLINED"  ❌
```

### 🟡 Test 4: Valid Transaction Over Threshold (Should Require Manual Approval)

```bash
curl -X POST http://localhost:8080/api/wallets/$WALLET_ID/transactions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 250.0,
    "merchant": "Lufthansa",
    "category": "Flights"
  }'

# Expected Response:
# {
#   "status": "PENDING",  🟡
#   "amount": 250.0,
#   ...
# }

# (Threshold was set to 200.0, so 250.0 > 200.0 = PENDING)
```

### ⏳ Test 5: AI Service Unavailable (Should Set PENDING_MANUAL_APPROVAL)

```bash
# Set AI to invalid endpoint
export AI_ENDPOINT="https://invalid-nonexistent-service.example.com/api"

# Restart backend
java -jar target/backend-0.0.1-SNAPSHOT.jar

# Create transaction
curl -X POST http://localhost:8080/api/wallets/$WALLET_ID/transactions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 100.0,
    "merchant": "Airline",
    "category": "Flights"
  }'

# Expected Response:
# {
#   "status": "PENDING_MANUAL_APPROVAL",  ⏳
#   ...
# }

# Alert should show: "AI service unavailable — requires manual approval"
```

---

## Verification Steps

### Check Transaction Status

```bash
# Get all transactions in wallet
curl -X GET http://localhost:8080/api/wallets/$WALLET_ID/transactions \
  -H "Authorization: Bearer $TOKEN"

# Get all transactions in group
curl -X GET http://localhost:8080/api/groups/$GROUP_ID/transactions \
  -H "Authorization: Bearer $TOKEN"
```

### Check Audit Logs

```bash
# Get audit logs for group
curl -X GET http://localhost:8080/api/groups/$GROUP_ID/audit-logs \
  -H "Authorization: Bearer $TOKEN"

# Look for AI_DECISION entries:
# {
#   "action": "AI_DECISION",
#   "user": "AI_SYSTEM",
#   "details": "AI Validation for transaction at Lidl Supermarket: DECLINED - Mismatch..."
# }
```

### Check Application Logs

```bash
# In another terminal, watch logs
tail -f logs/application.log | grep -i "AI\|ALERT\|VALIDATION"

# Should see:
# INFO ... AI validating transaction: Merchant=Lidl, Category=Grocery...
# WARN ... ALERT for group 'Travel Friends': Transaction Declined...
# INFO ... External AI decision: AI detected merchant mismatch
```

---

## Test Matrix

| Test Case | Merchant | Category | Wallet | Amount | Expected Status |
|-----------|----------|----------|--------|--------|-----------------|
| 1 (Valid) | Air France | Flights | Flight Budget | 100 | ✅ APPROVED |
| 2 (Invalid) | Lidl | Grocery | Flight Budget | 50 | ❌ DECLINED |
| 3 (Invalid) | Apple Store | Electronics | Flight Budget | 300 | ❌ DECLINED |
| 4 (Over threshold) | Lufthansa | Flights | Flight Budget | 250 | 🟡 PENDING |
| 5 (AI Down) | Any | Any | Any | 100 | ⏳ PENDING_MANUAL_APPROVAL |

---

## Expected Logs

### Successful Validation
```
INFO ... AI validating transaction: Merchant=Air France, Category=Flights for Wallet='Flight Budget'
INFO ... AI response: approved=true, reason=Merchant matches wallet purpose
INFO ... AI Validation for transaction at Air France: APPROVED - Transaction matches wallet purpose
```

### Declined Transaction
```
INFO ... AI validating transaction: Merchant=Lidl Supermarket, Category=Grocery for Wallet='Flight Budget'
WARN ... ALERT for group 'Travel Friends': Transaction Declined: €50.00 at 'Lidl Supermarket' on wallet 'Flight Budget' — Reason: Mismatch: Grocery/Supermarket on Travel/Flight wallet
```

### AI Service Unavailable
```
ERROR ... External AI service unavailable: Connection refused
INFO ... AI service unavailable — PENDING_MANUAL_APPROVAL
WARN ... ALERT for group 'Travel Friends': Transaction Declined: €100.00 at 'Airline' on wallet 'Flight Budget' — Reason: AI service unavailable — requires manual approval
```

---

## Troubleshooting

### Problem: "No authorization token supplied" 
**Solution:** Ensure `Authorization: Bearer $TOKEN` header is included

### Problem: "Wallet not found"
**Solution:** Verify `WALLET_ID` is correct; create a new wallet if needed

### Problem: "Status always APPROVED"
**Solution:** Check if AI is disabled (`ai.enabled: false`); check merchant/category match heuristic rules

### Problem: "Status always PENDING_MANUAL_APPROVAL"
**Solution:** Check AI endpoint is reachable; verify API key format

### Problem: "No alerts being sent"
**Solution:** Check application logs for "ALERT"; current implementation logs to console/file

---

## Performance Baseline

With heuristic rules (AI disabled):
- Transaction creation: ~50ms
- With valid merchant: Response time < 100ms
- With invalid merchant: Response time < 100ms

With real AI service:
- Depends on AI service latency + timeout (default 5000ms)
- Recommended: Use AI with timeout to prevent hanging

---

## Example Complete Flow (Copy-Paste)

```bash
#!/bin/bash

# Export for convenience
export API="http://localhost:8080/api"

# 1. Register
TOKEN=$(curl -s -X POST $API/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"testuser@test.com","password":"Test123456"}' | jq -r '.token')
echo "Token: $TOKEN"

# 2. Create group
GROUP_ID=$(curl -s -X POST $API/groups \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Group"}' | jq -r '.id')
echo "Group ID: $GROUP_ID"

# 3. Create wallet
WALLET_ID=$(curl -s -X POST $API/groups/$GROUP_ID/wallets \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Flight Budget","purpose":"Travel","budgetLimit":5000,"autoApproveThreshold":200}' | jq -r '.id')
echo "Wallet ID: $WALLET_ID"

# 4. Test valid transaction
echo "=== Valid Transaction ==="
curl -s -X POST $API/wallets/$WALLET_ID/transactions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount":100,"merchant":"Air France","category":"Flights"}' | jq '.status'

# 5. Test invalid transaction
echo "=== Invalid Transaction ==="
curl -s -X POST $API/wallets/$WALLET_ID/transactions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount":50,"merchant":"Lidl","category":"Grocery"}' | jq '.status'

echo "Done!"
```

Save as `test.sh`, run with `bash test.sh`

---

**All tests completed successfully?** → System is ready for production!
