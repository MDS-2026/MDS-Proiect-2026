# AI Transaction Validator - Configuration & Integration Guide

## Overview
The AI Transaction Validator automatically declines transactions where the merchant does not match the wallet's purpose, preventing fund misuse. It supports:
- **Real AI Service Integration**: Call external AI APIs with your endpoint and API key
- **Heuristic Fallback**: Uses rule-based validation if AI is unavailable
- **Graceful Degradation**: Transactions default to PENDING_MANUAL_APPROVAL if AI service fails

---

## How It Works

### Transaction Flow
1. **User initiates payment** on a virtual card
2. **TransactionService calls AiValidationService**
3. **AI validates** merchant/category against wallet purpose/name:
   - ✅ **APPROVED** → Auto-approve if amount ≤ wallet threshold, else PENDING
   - ❌ **DECLINED** → Instantly declined + alert sent to group
   - ⏳ **PENDING_MANUAL_APPROVAL** → If AI service is unavailable
4. **AlertService notifies** group members of declined/error states

### AI Validation Logic

#### Heuristic Rules (When AI disabled or unavailable)
- ❌ Grocery/Supermarket merchant on Travel/Flight wallet → **DECLINED**
- ❌ Electronics merchant on Food/Restaurant wallet → **DECLINED**  
- ❌ Entertainment merchant on Office wallet → **DECLINED**
- ✅ All other combinations → **APPROVED** (if within threshold)

#### Real AI Service (When configured)
Calls your AI endpoint with:
```json
{
  "walletName": "Flight Budget",
  "walletPurpose": "International travel expenses",
  "merchant": "Lidl Supermarket",
  "category": "Grocery",
  "amount": 50.0
}
```

Expected response:
```json
{
  "approved": false,
  "reason": "Grocery purchase not allowed on flight budget wallet",
  "confidence": 0.95
}
```

---

## Configuration

### Step 1: Set AI Endpoint & API Key

Edit `src/main/resources/application.yml`:

```yaml
ai:
  # Your AI service endpoint
  endpoint: https://your-ai-service.com/v1/validate
  # Your API key (will be used as Bearer token)
  api-key: sk-your-api-key-here
  # Optional: timeout for AI calls (default 5000ms)
  timeout-ms: 5000
  # Enable/disable AI validation (true = use AI, false = use heuristics)
  enabled: true
```

### Step 2: Environment Variables (Recommended for Production)

Instead of hardcoding in `application.yml`, use environment variables:

**On Linux/Mac:**
```bash
export AI_ENDPOINT="https://your-ai-service.com/v1/validate"
export AI_API_KEY="sk-your-api-key-here"
```

**In Docker:**
```bash
docker run -e AI_ENDPOINT="https://your-ai-service.com/v1/validate" \
           -e AI_API_KEY="sk-your-api-key-here" \
           -p 8080:8080 mds-proiect:latest
```

**In docker-compose.yml:**
```yaml
services:
  backend:
    image: mds-proiect:latest
    ports:
      - "8080:8080"
    environment:
      AI_ENDPOINT: https://your-ai-service.com/v1/validate
      AI_API_KEY: sk-your-api-key-here
    depends_on:
      - postgres
```

### Step 3: Run Application

```bash
# Build
./mvnw clean package -DskipTests

# Run with environment variables
export AI_ENDPOINT="https://your-ai-service.com/v1/validate"
export AI_API_KEY="sk-your-api-key-here"
java -jar target/backend-0.0.1-SNAPSHOT.jar

# OR run with inline environment
AI_ENDPOINT="https://..." AI_API_KEY="sk-..." java -jar target/backend-0.0.1-SNAPSHOT.jar
```

---

## Testing

### Test 1: Transaction with AI Approval
```bash
curl -X POST http://localhost:8080/api/wallets/{walletId}/transactions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 100.0,
    "merchant": "Air France",
    "category": "Flights"
  }'

# Response: status = "APPROVED" (if amount ≤ threshold)
```

### Test 2: Transaction with Mismatch (Auto-Declined)
```bash
curl -X POST http://localhost:8080/api/wallets/{walletId}/transactions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 50.0,
    "merchant": "Lidl Supermarket",
    "category": "Grocery"
  }'

# Wallet: "Flight Budget" (purpose: "Travel")
# Response: status = "DECLINED"
# Alert sent to all group members with reason
```

### Test 3: AI Service Unavailable
```bash
# Set invalid AI endpoint
export AI_ENDPOINT="https://invalid-service.com/api"

curl -X POST http://localhost:8080/api/wallets/{walletId}/transactions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 50.0,
    "merchant": "Amazon",
    "category": "Electronics"
  }'

# Response: status = "PENDING_MANUAL_APPROVAL"
# Alert sent: "AI service unavailable — requires manual approval"
```

---

## API Endpoints

### Create Transaction (with AI Validation)
```
POST /api/wallets/{walletId}/transactions
Authorization: Bearer <token>
Content-Type: application/json

{
  "amount": 100.0,
  "merchant": "Merchant Name",
  "category": "Category Name"
}
```

**Response:**
```json
{
  "id": "uuid",
  "amount": 100.0,
  "merchant": "Merchant Name",
  "category": "Category Name",
  "status": "APPROVED|DECLINED|PENDING|PENDING_MANUAL_APPROVAL",
  "walletId": "uuid",
  "walletName": "Wallet Name",
  "createdAt": "2026-05-01T13:00:00Z"
}
```

### Get Group Transactions
```
GET /api/groups/{groupId}/transactions
Authorization: Bearer <token>
```

---

## Transaction States

| State | Meaning |
|-------|---------|
| **APPROVED** | Transaction auto-approved (AI validated, amount ≤ threshold) |
| **DECLINED** | Transaction auto-declined due to merchant/category mismatch |
| **PENDING** | Awaiting manual approval (AI approved but amount > threshold) |
| **PENDING_MANUAL_APPROVAL** | AI service unavailable — requires manual review |

---

## Audit Logging

All AI decisions are logged in the audit table:
```
AuditAction: AI_DECISION
User: AI_SYSTEM
Message: "AI Validation for transaction at [Merchant]: [APPROVED|DECLINED] - [Reason]"
```

Example:
```
AI Validation for transaction at Lidl Supermarket: DECLINED - Mismatch: Grocery/Supermarket on Travel/Flight wallet
```

---

## Alert Notifications

When a transaction is declined or AI unavailable, all group members are alerted with:
- Merchant name
- Amount
- Wallet name
- Decline reason

**Current Implementation**: Logs to application logs
**TODO**: Integrate with email/SMS/push notification services

---

## Files Modified/Created

| File | Change |
|------|--------|
| `src/main/resources/application.yml` | Added AI configuration section |
| `src/main/java/.../services/AiValidationService.java` | Enhanced with AI token support |
| `src/main/java/.../services/AlertService.java` | NEW - Alerts group on declined transactions |
| `src/main/java/.../services/TransactionService.java` | Updated to handle AI exceptions |
| `src/main/java/.../models/enums/TransactionStatus.java` | Added PENDING_MANUAL_APPROVAL |
| `src/main/java/.../models/enums/AuditAction.java` | Added AI_DECISION |
| `src/main/java/.../config/SecurityConfig.java` | Added RestTemplate bean |

---

## Next Steps

1. **Provide AI endpoint & API key** when ready
2. **Update application.yml** with your credentials
3. **Test the flow** with sample transactions
4. **Integrate notification service** (email/SMS/push)
5. **Add admin UI** for manual approval of PENDING_MANUAL_APPROVAL transactions

---

## Support

For issues or questions:
- Check application logs: `tail -f logs/application.log`
- Verify AI endpoint is reachable: `curl -X GET https://your-ai-service.com/health`
- Ensure AI_API_KEY format matches your service requirements
- Test AI response format with Postman before deploying

