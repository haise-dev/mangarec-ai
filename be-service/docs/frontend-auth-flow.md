# Frontend Auth Flow

Base URL dev: `http://localhost:8080`

All auth responses use:

```json
{
  "status": 200,
  "message": "Login successful",
  "data": {}
}
```

## Register

`POST /api/v1/auth/register`

```json
{
  "email": "reader@example.com",
  "password": "StrongPass123",
  "name": "Manga Reader",
  "guestId": "guest-cookie-id"
}
```

`guestId` is optional. Send it when the user started as guest so backend can claim guest history.

## Login

`POST /api/v1/auth/login`

```json
{
  "email": "reader@example.com",
  "password": "StrongPass123",
  "deviceId": "browser-device-id"
}
```

Store `data.accessToken` for API calls:

```http
Authorization: Bearer <accessToken>
```

Store `data.refreshToken` securely for later refresh-token API when implemented.

## Google Login

`POST /api/v1/auth/google`

```json
{
  "idToken": "<google-id-token>",
  "guestId": "guest-cookie-id",
  "deviceId": "browser-device-id"
}
```

Frontend gets `idToken` from Google Identity Services using the same web client ID configured in backend as `GOOGLE_AUTH_CLIENT_ID`.

If Google email is verified:
- existing account with same email is linked to Google;
- otherwise backend creates a new user;
- response returns MangaRec JWT tokens.

## Forgot Password

`POST /api/v1/auth/forgot-password`

```json
{
  "email": "reader@example.com"
}
```

Backend always returns success-style response to avoid revealing whether an email exists. If the account exists, backend sends a 6-digit OTP to the email.

## Reset Password

`POST /api/v1/auth/reset-password`

```json
{
  "email": "reader@example.com",
  "otp": "123456",
  "newPassword": "NewStrongPass123",
  "confirmPassword": "NewStrongPass123"
}
```

If OTP is valid, backend updates local password. Google-only users can also create a local password through this flow.

## Error Handling

Common statuses:

- `400`: validation error
- `401`: invalid credentials, invalid token, invalid Google id token
- `409`: duplicate email or conflicting data
- `500`: unexpected server error

