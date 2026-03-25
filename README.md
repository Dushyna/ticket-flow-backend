# Ticketflow Project Description

## Chapter 1. Current State

### 1. Security and Authorization

To protect access to Ticketflow API and resources, OAuth2 and JWT (JSON Web Tokens) are used, with two types of tokens:

* **Access Token (AT)**

    * Short-lived token (usually 5–15 minutes).
    * Stored in an HTTP-only cookie (HttpOnly).
    * Used to authorize each request to protected routes.
    * Minimizes the risk of compromise: if intercepted, it expires quickly.

* **Refresh Token (RT)**

    * Long-lived token (usually several days or weeks). 
    * Also stored in an HTTP-only cookie.
    * Used to obtain a new Access Token without re-entering credentials.
    * Allows maintaining a user session without constant authentication.
  
**How it works:**:

1. On login (/api/v1/auth/login), the server checks the email and password and issues a token pair (AT + RT) in two cookies.
2. The browser automatically sends the Access Token from the HTTP-only cookie with every request to protected routes.
3. When the AT expires, the client automatically calls /api/v1/auth/refresh with the RT to receive a new AT.
4. On logout (/api/v1/auth/logout), both cookies are cleared.

**Authorization Routes:**:

* **Register a Business Owner (Tenant)**
  `POST /api/v1/auth/register/tenant`
  Open endpoint for creating a new user-Business Owner.
* **Registration customer**
  `POST /api/v1/auth/register/customer`
  Open endpoint for creating a new user-customer.
* **Registration Confirmation**
  `GET  /api/v1/auth/confirm/{code}`
  Open endpoint to activate the account via a code.
* **Login**
  `POST /api/v1/auth/login`
  Accepts email and password, returns JWT in an HTTP-only cookie.
* **Refresh Token**
  `POST /api/v1/auth/refresh-token`
  Accepts refresh token, returns a new access token.
* **Get current authenticated user**
  `POST /api/v1/auth/me`
  Returns current user based on access token stored in httpOnly cookie.
* **Forgot password**
  `GET /api/v1/auth/forgot-password`
  Sends an email to change your password.
* **Validate reset password token**
  `GET /api/v1/auth/reset-password/validate`
  Open endpoint for validating your token.
* **Change password**
  `POST /api/v1/auth/reset-password`
  Open endpoint for changing your password.
* **Logout**
  `POST /api/v1/auth/logout`
  Invalidates tokens and clears cookies.

**Роли пользователей**:

* **ROLE\SUPER_ADMIN** — full administrative rights: manage users, rooms, and settings.
* **ROLE\TENANT_ADMIN** — administrative rights: manage own rooms and settings.
* **ROLE\_USER** — basic access: create and edit own projects.

---

## CI/CD

![CI](https://github.com/<your-org>/<your-repo>/actions/workflows/gradle.yml/badge.svg)

The project is integrated with **GitHub Actions** for automatic build, testing, 
and static analysis on every push to main, develop branches, and on Pull Requests.

### Workflow файл

Located at `.github/workflows/gradle.yml`. Triggered on:

* `push` to `main`, `develop` branches
* `pull_request` to `main`, `develop` branches

#### Main Steps

1. **Checkout** — clone the repository.
2. **Set up JDK 21**:

   ```yaml
   - name: Set up JDK
     uses: actions/setup-java@v3
     with:
       java-version: 21
   ```
3. **Cache Gradle dependencies**.
4. **Start PostgreSQL** (for integration tests)
5. **Build and run tests:**:

   ```bash
   ./gradlew clean build --info
   ```
---
## Frontend
https://github.com/Dushyna/ticket-flow-frontend