# Postman Test Data for Fraud-Safe Digital Banking System

## Base Configuration

| Environment Variable | Value |
|---------------------|-------|
| `base_url` | `http://localhost:8080` |
| `auth_token` | *(set after login)* |

---

## 1. Authentication (User Service)

### Register New User
```http
POST {{base_url}}/api/auth/register
Content-Type: application/json
```

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "username": "johndoe",
  "email": "john.doe@example.com",
  "password": "Password123!",
  "confirmPassword": "Password123!",
  "role": "CUSTOMER"
}
```

### Login
```http
POST {{base_url}}/api/auth/login
Content-Type: application/json
```

```json
{
  "username": "johndoe",
  "password": "Password123!"
}
```

> **Note**: Save the `accessToken` from response as `{{auth_token}}` environment variable.

### Get Current User Profile
```http
GET {{base_url}}/api/auth/me
Authorization: Bearer {{auth_token}}
```

### Logout
```http
POST {{base_url}}/api/auth/logout
Authorization: Bearer {{auth_token}}
```

---

## 2. Account Service

### Create New Account
```http
POST {{base_url}}/api/accounts
Authorization: Bearer {{auth_token}}
Content-Type: application/json
Idempotency-Key: {{uuid}}
```

```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "type": "CHECKING",
  "balance": "1000.00",
  "currency": "USD",
  "idempotencyKey": "idem-550e8400-e29b-41d4-a716-446655440000"
}
```

### Get Account Details
```http
GET {{base_url}}/api/accounts/ACC-1234567890?page=0&size=20
Authorization: Bearer {{auth_token}}
```

### Reserve Balance (Admin/Manager)
```http
POST {{base_url}}/api/accounts/ACC-1234567890/balance/reserve
Authorization: Bearer {{admin_token}}
Content-Type: application/json
```

```json
{
  "transactionId": "660e8400-e29b-41d4-a716-446655440001",
  "sagaId": "770e8400-e29b-41d4-a716-446655440002",
  "amount": "500.00",
  "description": "Reserve for pending transaction"
}
```

### Release Balance Reservation (Admin/Manager)
```http
POST {{base_url}}/api/accounts/ACC-1234567890/balance/release
Authorization: Bearer {{admin_token}}
Content-Type: application/json
```

```json
{
  "transactionId": "660e8400-e29b-41d4-a716-446655440001",
  "sagaId": "770e8400-e29b-41d4-a716-446655440002",
  "amount": "500.00",
  "description": "Release reservation after completion"
}
```

### Debit Balance (Admin/Manager)
```http
POST {{base_url}}/api/accounts/ACC-1234567890/balance/debit
Authorization: Bearer {{admin_token}}
Content-Type: application/json
```

```json
{
  "transactionId": "660e8400-e29b-41d4-a716-446655440001",
  "sagaId": "770e8400-e29b-41d4-a716-446655440002",
  "amount": "250.00",
  "description": "Debit for completed payment"
}
```

### Credit Balance (Admin/Manager)
```http
POST {{base_url}}/api/accounts/ACC-1234567890/balance/credit
Authorization: Bearer {{admin_token}}
Content-Type: application/json
```

```json
{
  "transactionId": "660e8400-e29b-41d4-a716-446655440001",
  "sagaId": "770e8400-e29b-41d4-a716-446655440002",
  "amount": "100.00",
  "description": "Credit for refund"
}
```

---

## 3. Transaction Service

### Initiate Transaction
```http
POST {{base_url}}/api/v1/transactions
Authorization: Bearer {{auth_token}}
Content-Type: application/json
```

```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "senderAccountNo": "ACC-1234567890",
  "receiverAccountNo": "ACC-0987654321",
  "amount": "250.00",
  "transactionType": "TRANSFER",
  "accountType": "CHECKING",
  "description": "Monthly rent payment"
}
```

### Get Transaction by Reference
```http
GET {{base_url}}/api/v1/transactions/TXN-abc123def456
Authorization: Bearer {{auth_token}}
```

### Get Transaction by ID
```http
GET {{base_url}}/api/v1/transactions/id/880e8400-e29b-41d4-a716-446655440003
Authorization: Bearer {{auth_token}}
```

---

## 4. Payment Service

### Create Payment Order
```http
POST {{base_url}}/api/v1/payments
Authorization: Bearer {{auth_token}}
Content-Type: application/json
Idempotency-Key: {{uuid}}
```

```json
{
  "merchantId": "990e8400-e29b-41d4-a716-446655440004",
  "customerAccountNo": "ACC-1234567890",
  "amount": "99.99",
  "currency": "USD",
  "idempotencyKey": "pay-idem-990e8400-e29b-41d4-a716-446655440004"
}
```

### Get Payment Order
```http
GET {{base_url}}/api/v1/payments/ORD-xyz789uvw012
Authorization: Bearer {{auth_token}}
```

### Process Payment (Checkout)
```http
POST {{base_url}}/api/v1/payments/ORD-xyz789uvw012/pay
Authorization: Bearer {{auth_token}}
```

### Subscribe to Webhooks (Merchant)
```http
POST {{base_url}}/api/v1/webhooks/subscribe
Authorization: Bearer {{merchant_token}}
Content-Type: application/json
```

```json
{
  "merchantId": "990e8400-e29b-41d4-a716-446655440004",
  "targetUrl": "https://merchant.example.com/webhooks/payment",
  "secretKey": "whsec_abcdefghijklmnopqrstuvwxyz123456",
  "eventTypes": "payment.completed,payment.failed,payment.refunded"
}
```

---

## 5. Help Desk (AI Agent)

### Chat with AI Agent (Server-Sent Events)
```http
POST {{base_url}}/api/agent/chat
Content-Type: application/json
Accept: text/event-stream
```

```json
{
  "prompt": "What is my account balance?",
  "conversationId": "conv-110e8400-e29b-41d4-a716-446655440005"
}
```

### Health Check
```http
GET {{base_url}}/api/agent/health
```

---

## 6. Example UUIDs for Testing

| Variable | Value |
|----------|-------|
| `userId` | `550e8400-e29b-41d4-a716-446655440000` |
| `transactionId` | `660e8400-e29b-41d4-a716-446655440001` |
| `sagaId` | `770e8400-e29b-41d4-a716-446655440002` |
| `merchantId` | `990e8400-e29b-41d4-a716-446655440004` |
| `conversationId` | `110e8400-e29b-41d4-a716-446655440005` |
| `paymentOrderId` | `880e8400-e29b-41d4-a716-446655440003` |

---

## 7. Account Types

| Type | Description |
|------|-------------|
| `CHECKING` | Standard checking account |
| `SAVINGS` | Savings account |
| `CREDIT` | Credit account |
| `INVESTMENT` | Investment account |

---

## 8. Transaction Types

| Type | Description |
|------|-------------|
| `TRANSFER` | Account-to-account transfer |
| `DEPOSIT` | Cash/check deposit |
| `WITHDRAWAL` | Cash withdrawal |
| `PAYMENT` | Bill/merchant payment |
| `REFUND` | Refund transaction |
| `FEE` | Service fee |

---

## 9. Test Flow Example

### Complete User Journey

1. **Register** → Get userId
2. **Login** → Get `auth_token`
3. **Create Account** → Get `accountNo`
4. **Create Payment Order** → Get `orderReference`
5. **Process Payment** → Verify completion
6. **Check Transaction** → Verify transaction record

### Admin Journey

1. **Login as Admin** → Get `admin_token`
2. **Reserve Balance** → For pending transaction
3. **Debit Balance** → After fraud check passes
4. **Release/Adjust** → As needed

---

## 10. Headers Reference

### Common Headers
```http
Authorization: Bearer {{auth_token}}
Content-Type: application/json
Accept: application/json
```

### Idempotency (for create operations)
```http
Idempotency-Key: {{uuid}}
```

### Webhook Signature (incoming webhooks)
```http
X-Webhook-Signature: sha256=...
X-Webhook-Timestamp: 1699999999
X-Webhook-Event-Type: payment.completed
```