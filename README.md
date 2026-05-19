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

**User Management Routes** (Requires Authentication):

* **Get Current User Details**
  `GET /api/v1/users/me-details`
  Returns extended profile details for the currently logged-in user.
* **Update Current User Profile**
  `PATCH /api/v1/users/update-user`
  Allows the authenticated user to update their own profile details (e.g., name, phone).
* **Get All Managed Users**
  `GET /api/v1/users/all`
  Returns a list of all users within the administrator's context hierarchy. **Restricted to:** `ROLE_SUPER_ADMIN`, `ROLE_TENANT_ADMIN`.
* **Update User Role**
  `PATCH /api/v1/users/role`
  Allows updating a user's role within the organization's scope. **Restricted to:** `ROLE_SUPER_ADMIN`, `ROLE_TENANT_ADMIN`.

**Cinema Management Routes**:

* **Create Cinema**
  `POST /api/v1/cinemas`
  Registers a new cinema building linked to the authenticated tenant's organization. **Restricted to:** `ROLE_TENANT_ADMIN`.
* **Update Cinema Details**
  `PATCH /api/v1/cinemas/{id}`
  Partially updates a specific cinema's information using its UUID. **Restricted to:** `ROLE_TENANT_ADMIN`.
* **Get All Cinemas**
  `GET /api/v1/cinemas`
  Returns a public list of active cinemas for guests, or an organization-specific list for tenants. **Open to:** All users/Guests (`permitAll`).
* **Get Cinema Details by ID**
  `GET /api/v1/cinemas/{id}`
  Retrieves comprehensive information about a specific cinema by its UUID. **Open to:** All users/Guests (`permitAll`).
* **Delete a Cinema**
  `DELETE /api/v1/cinemas/{id}`
  Permanently removes a cinema building from the system. **Restricted to:** `ROLE_TENANT_ADMIN`.

**Movie Management Routes**:

* **Add New Movie**
  `POST /api/v1/movies`
  Creates a new movie entry in the global catalog. **Restricted to:** `ROLE_TENANT_ADMIN`.
* **Update Movie Details**
  `PUT /api/v1/movies/{id}`
  Updates an existing movie's information by its UUID using a full payload replacement. **Restricted to:** `ROLE_TENANT_ADMIN`.
* **Get All Movies**
  `GET /api/v1/movies`
  Returns a list of all movies in the catalog for both viewers and owners. **Open to:** All users/Guests (`permitAll`).
* **Get Movie by ID**
  `GET /api/v1/movies/{id}`
  Retrieves detailed catalog information about a specific movie using its UUID. **Open to:** All users/Guests (`permitAll`).
* **Delete a Movie**
  `DELETE /api/v1/movies/{id}`
  Permanently removes a movie from the catalog. **Restricted to:** `ROLE_TENANT_ADMIN`.

**Movie Hall Management Routes**:

* **Create Hall Layout**
  `POST /api/v1/halls`
  Saves rows, columns, and JSON configurations for a new cinema hall. **Restricted to:** `ROLE_TENANT_ADMIN`.
* **Get All Halls for a Cinema**
  `GET /api/v1/halls/cinema/{cinemaId}`
  Returns a list of all movie halls linked to a specific cinema building using its UUID. **Open to:** All users/Guests (`permitAll`).
* **Get Hall Details by ID**
  `GET /api/v1/halls/{id}`
  Retrieves detailed configurations, rows, and dimensions of a specific movie hall by its UUID. **Open to:** All users/Guests (`permitAll`).
* **Update Existing Hall Layout**
  `PATCH /api/v1/halls/{id}`
  Partially updates a specific movie hall's layout configuration. **Restricted to:** `ROLE_TENANT_ADMIN`.
* **Delete Movie Hall**
  `DELETE /api/v1/halls/{id}`
  Permanently removes a movie hall from the cinema system catalog. **Restricted to:** `ROLE_TENANT_ADMIN`.

**Showtime Management Routes**:

* **Schedule New Showtime**
  `POST /api/v1/showtimes`
  Creates a new movie session in a specific hall. Includes automated validation to prevent time overlaps. **Restricted to:** `ROLE_TENANT_ADMIN`.
* **Update Existing Showtime**
  `PATCH /api/v1/showtimes/{id}`
  Updates scheduling data or movie/hall bounds for an existing session by its UUID. **Restricted to:** `ROLE_TENANT_ADMIN`.
* **Delete a Showtime**
  `DELETE /api/v1/showtimes/{id}`
  Permanently removes a scheduled session from the catalog. **Restricted to:** `ROLE_TENANT_ADMIN`.
* **Get Showtime by ID**
  `GET /api/v1/showtimes/{id}`
  Retrieves comprehensive metadata and pricing schema for a specific session. **Open to:** All users/Guests (`permitAll`).
* **Get All Showtimes for a Movie**
  `GET /api/v1/showtimes/movie/{movieId}`
  Returns a chronological list of active upcoming sessions for a specific movie. Automatically filters out past showtimes. **Open to:** All users/Guests (`permitAll`).
* **Get All Showtimes for a Hall**
  `GET /api/v1/showtimes/hall/{hallId}`
  Returns a list of scheduled sessions linked to a specific movie hall. **Open to:** All users/Guests (`permitAll`).
* **Get All Showtimes for a Cinema**
  `GET /api/v1/showtimes/cinema/{cinemaId}`
  Returns a complete timetable of upcoming available sessions for a cinema building. Automatically filters out past showtimes. **Open to:** All users/Guests (`permitAll`).

**Ticket Types & Pricing Routes**:

* **Create Ticket Type**
  `POST /api/v1/ticket-types`
  Defines a new ticket category (e.g., 'Student', 'VIP') with specific pricing multipliers or discounts. **Restricted to:** `ROLE_TENANT_ADMIN`.
* **Update Ticket Type**
  `PATCH /api/v1/ticket-types/{id}`
  Partially updates an existing ticket category's fields or multiplier by its UUID. **Restricted to:** `ROLE_TENANT_ADMIN`.
* **Get Tenant's Ticket Types**
  `GET /api/v1/ticket-types/my`
  Returns all ticket pricing categories configured by the authenticated business owner's organization. **Restricted to:** `ROLE_TENANT_ADMIN`.
* **Get Ticket Types by Organization ID**
  `GET /api/v1/ticket-types/organization/{orgId}`
  Public endpoint to fetch active pricing categories and modifiers for a specific cinema chain. **Open to:** All users/Guests (`permitAll`).
* **Delete Ticket Type**
  `DELETE /api/v1/ticket-types/{id}`
  Permanently removes a ticket pricing category from the organization's scope. **Restricted to:** `ROLE_TENANT_ADMIN`.

**Booking & Payment Routes** (Requires Authentication unless specified):

* **Create Bookings and Get Payment URL**
  `POST /api/v1/bookings`
  Books multiple seats for the authenticated user and returns a Stripe payment URL. Includes unique DB constraint locks to prevent concurrent seat selection. **Restricted to:** Logged-in users (`isAuthenticated`).
* **Get Occupied Seats**
  `GET /api/v1/bookings/occupied/{showtimeId}`
  Returns a list of taken row/col coordinates for a specific showtime. Automatically filters out expired locks. **Open to:** All users/Guests (`permitAll`).
* **Get My Bookings**
  `GET /api/v1/bookings/my`
  Retrieves the authenticated user's complete personal ticket purchase and booking history. **Restricted to:** Logged-in users (`isAuthenticated`).
* **Get Order Status by Session ID**
  `GET /api/v1/bookings/status/{sessionId}`
  Fetches the latest order and payment status from the DB using the Stripe Session ID. **Restricted to:** Logged-in users (`isAuthenticated`).
* **Box Office Sale (Offline)**
  `POST /api/v1/bookings/box-office`
  Executes an immediate physical ticket purchase directly at the cinema box office without Stripe redirect. **Restricted to:** `ROLE_CASHIER`, `ROLE_TENANT_ADMIN`, `ROLE_SUPER_ADMIN`.
* **Get Payment URL for Existing Order**
  `GET /api/v1/bookings/payment-url/{orderId}`
  Regenerates and returns a Stripe checkout session URL for an existing pending order. **Restricted to:** Logged-in users (`isAuthenticated`).
* **Verify Ticket at Entrance (QR-Code Scan)**
  `POST /api/v1/bookings/verify-entrance/{bookingId}`
  Validates a ticket's status when scanned at the cinema entrance gate. **Restricted to:** `ROLE_CASHIER`, `ROLE_TENANT_ADMIN`, `ROLE_SUPER_ADMIN`.
* **Get Cashier Sales History**
  `GET /api/v1/bookings/cashier/history`
  Retrieves a detailed log of offline sales handled by the current staff context. **Restricted to:** `ROLE_SUPER_ADMIN`, `ROLE_CASHIER`, `ROLE_TENANT_ADMIN`, `ROLE_CONTROLLER`.

**Ticket Documents Routes**:

* **Download Printable PDF Order Tickets Book**
  `GET /api/v1/tickets/download/order/{orderId}`
  Generates and downloads a printable PDF document containing all tickets for a specific order by its UUID (`produces = application/pdf`). **Open to:** `ROLE_SUPER_ADMIN`, `ROLE_TENANT_ADMIN`, `ROLE_CASHIER`, or public users (`permitAll()`).

### 👥 User Roles and Permissions

The system implements Role-Based Access Control (RBAC). The following roles are available:

* **ROLE_SUPER_ADMIN** — Full administrative access: manage platform configurations, users, halls, and system-wide settings.
* **ROLE_TENANT_ADMIN** — Business owner access: manage their own organization, specific movie halls, showtimes, and tenant settings.
* **ROLE_CASHIER** — Ticket sales access: authorized to sell and issue tickets directly at the cinema box office.
* **ROLE_CONTROLLER** — Ticket validation access: authorized to scan and check ticket validity (e.g., via QR codes) at the entrance.
* **ROLE_USER** — Basic customer access: browse active movies and showtimes, select seats, and purchase tickets online.

---
## 💳 Stripe Payment Setup

This project uses Stripe for ticket purchasing. To configure Stripe integration, follow these steps:

### 1. Create a Stripe Account
1. Sign up at [Stripe Dashboard](https://stripe.com).
2. Switch to **Test Mode** in the top right corner.

### 2. Configure Environment Variables
Do not hardcode your credentials! Create an `application-local.yaml` file or set the following environment variables on your system/Docker:

* `STRIPE_API_KEY` - Your **Secret key** (starts with `sk_test_...`). Found in *Developers -> API keys*.
* `STRIPE_WEBHOOK_SECRET` - Your Webhook signing secret (starts with `whsec_...`). Found after setting up the webhook.

### 3. Local Webhook Testing (Crucial for Booking Cleanup & Confirmation)
Since Stripe needs to send events back to your local Spring Boot application (`localhost:8080`), you need to forward webhook events:

1. Install the **Stripe CLI** following the [official guide](https://stripe.com).
2. Log in to your account via terminal:
   ```bash
   stripe login
   ```
3. Start forwarding events to your local endpoint:
   ```bash
   stripe listen --forward-to localhost:8080/api/v1/payments/webhook
   ```
   *(Note: Adjust the path if your webhook endpoint URI is different).*
4. The CLI will print a local webhook secret (e.g., `whsec_...`). Copy this value and set it as your `STRIPE_WEBHOOK_SECRET` variable.
5. Restart your Spring Boot application


## ⚙️ Environment Variables Configuration

To run this project locally, you must configure several environment variables. You can set them in your system, your IDE (IntelliJ IDEA Run Configuration), or by creating a local profile.


### 1. Database Configuration
* `DB_USERNAME` - Your PostgreSQL database username.
* `DB_PASSWORD` - Your PostgreSQL database password.

### 2. JWT Security Credentials
* `JWT_AT_SECRET` - A Base64 encoded secret phrase for Access Tokens (must be at least 32 characters long).
* `JWT_RT_SECRET` - A Base64 encoded secret phrase for Refresh Tokens (must be at least 32 characters long).

### 3. 📧 Email Service Setup

The system automatically sends verification codes for account confirmation and password reset links. You can configure this service using either **Gmail (for real emails)** or **Mailtrap (for safe development/testing)**.

Choose one of the following setups and configure the environment variables:

#### Option A: Safe Testing via Mailtrap (Recommended for Development)
Mailtrap intercepts all outgoing emails and displays them in a fake inbox. It prevents sending accidental spam to real addresses.

1. Sign up for free at [Mailtrap.io](https://mailtrap.io).
2. Go to **Inboxes** -> **My Inbox** -> **Integration** and select **External SMTP**.
3. Set the following environment variables using the credentials provided by Mailtrap:
    * `EMAIL_HOST` = `sandbox.smtp.mailtrap.io`
    * `EMAIL_USERNAME` = `your_mailtrap_username`
    * `EMAIL_PASSWORD` = `your_mailtrap_password`

#### Option B: Real Email Delivery via Gmail
If you want the system to send actual emails to users, you can use a personal or brand Gmail account. Note: Your regular Gmail password **will not work** due to Google's security policy. You must generate an **App Password**.

1. Go to your [Google Account Settings](https://google.com).
2. Enable **2-Step Verification** (Mandatory for App Passwords).
3. Search for **App Passwords** in the top search bar.
4. Enter a name (e.g., `TicketFlow SaaS Application`) and click **Create**.
5. Google will display a **16-character password** (e.g., `abcd efgh ijkl mnop`). Copy it!
6. Set the following environment variables:
    * `EMAIL_HOST` = `smtp.gmail.com`
    * `EMAIL_USERNAME` = `your_actual_email@gmail.com`
    * `EMAIL_PASSWORD` = `your_16_character_app_password_without_spaces`

*(Note: Do not create a custom corporate email like `info@yoursite.com` during development. A free Gmail or Mailtrap account is more than enough to completely run and test the system).*

### 4. 🔑 Google OAuth2 Integration
Required for Social Login authentication options:
* `CLIENT_ID` - Your Google Cloud Console OAuth2 Client Identifier.
* `CLIENT_SECRET` - Your Google Cloud Console OAuth2 Client Secret Key.

### 5. 💳 Stripe Payments Setup
Required to initialize checkout pages and handle booking webhooks:
* `STRIPE_API_KEY` - Your Stripe Secret Key starting with `sk_test_...` (*Developers -> API keys*).
* `STRIPE_WEBHOOK_SECRET` - Your Stripe signing secret starting with `whsec_...` (obtained after starting the Stripe CLI forwarding via `stripe listen`).

### ☁️ File Storage (DigitalOcean Spaces)
By default, for local development, the system safe-fails with mock values (`fake`). If you want to connect actual S3 storage, provide:
* `DO_SPACES_ACCESS_KEY`, `DO_SPACES_SECRET_KEY`, `DO_SPACES_BUCKET`


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