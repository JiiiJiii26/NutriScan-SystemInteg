# NutriScan: Barcode-Based Food and Calorie Diary

## Project Overview
NutriScan is a beginner-friendly web application tailored for Filipino college students to monitor their daily caloric and nutritional intake from packaged foods (such as instant noodles, canned goods, and student snacks). This repository contains **Increment 1**, focusing strictly on **User Accounts and Authentication**—implementing secure student registration, BCrypt password hashing, session-based authentication, Google OAuth 2.0 single sign-on, and a role-protected dashboard for the `STUDENT` role.

---

## Tech Stack
- **Frontend:** ReactJS (Vite, plain JavaScript — no TypeScript, no heavy UI framework)
- **Backend:** Spring Boot 3.3 (Java 17, Maven, single module)
- **Database:** PostgreSQL 14+ (table: `users`)
- **Authentication:** Google OAuth 2.0 (primary) + Local Email/Password fallback (session-based cookies: `JSESSIONID`)
- **Password Security:** BCrypt password hashing via Spring Security (strength factor 10)

---

## Directory Structure
```
Nutriscan/
├── backend/
│   ├── pom.xml                                   # Maven build configuration
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/nutriscan/
│   │   │   │   ├── NutriScanApplication.java     # Main entry point & PasswordEncoder bean
│   │   │   │   ├── config/
│   │   │   │   │   ├── CorsConfig.java           # Allows localhost:5173 with credentials
│   │   │   │   │   └── SecurityConfig.java       # Session auth, OAuth2, RBAC (ROLE_STUDENT)
│   │   │   │   ├── controller/
│   │   │   │   │   ├── AuthController.java       # /api/auth/register, login, logout, /api/me
│   │   │   │   │   └── HomeController.java       # Root status & OAuth redirects
│   │   │   │   ├── dto/
│   │   │   │   │   ├── LoginRequest.java         # Login form payload
│   │   │   │   │   ├── RegisterRequest.java      # Registration payload with validations
│   │   │   │   │   └── UserResponse.java         # Safe user profile response
│   │   │   │   ├── model/
│   │   │   │   │   └── User.java                 # JPA Entity for 'users' table
│   │   │   │   ├── repository/
│   │   │   │   │   └── UserRepository.java       # JpaRepository for PostgreSQL
│   │   │   │   └── service/
│   │   │   │       ├── CustomUserDetailsService.java # Local login loader
│   │   │   │       └── UserService.java          # Registration, OAuth auto-creation
│   │   │   └── resources/
│   │   │       ├── application.properties        # Main PostgreSQL and OAuth configs
│   │   │       └── schema.sql                    # Database table DDL
│   │   └── test/                                 # Unit & Integration tests (H2 in-memory)
├── frontend/
│   ├── index.html
│   ├── package.json                              # React 18 + react-router-dom v6 + Vite
│   ├── vite.config.js                            # Dev server on port 5173
│   └── src/
│       ├── main.jsx                              # React root mounting
│       ├── App.jsx                               # Router configuration
│       ├── index.css                             # Mobile-friendly clean stylesheet
│       ├── api.js                                # Fetch wrapper with credentials: 'include'
│       ├── context/
│       │   └── AuthContext.jsx                   # Central auth state & /api/me session checks
│       ├── components/
│       │   ├── Navbar.jsx                        # Student navbar with role badge & logout
│       │   └── ProtectedRoute.jsx                # Guards /dashboard (checks role & session)
│       └── pages/
│           ├── Login.jsx                         # Google login button + email/password form
│           ├── Register.jsx                      # Full name, email, password, confirm pass
│           └── Dashboard.jsx                     # Welcome greeting & placeholder cards
├── database/
│   └── schema.sql                                # DDL script for PostgreSQL
├── .env.example                                  # Template for Google OAuth environment variables
├── .gitignore                                    # Excludes node_modules, target, .env
└── README.md
```

---

## API Endpoints (Increment 1)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Registers a local student account, hashes password with BCrypt |
| `POST` | `/api/auth/login` | Public | Authenticates credentials, creates HTTP session, returns `JSESSIONID` |
| `POST` | `/api/auth/logout` | Authenticated | Invalidates session and clears cookie |
| `GET` | `/api/me` | Authenticated (`ROLE_STUDENT`) | Returns current student profile (returns `401 Unauthorized` if unauthenticated) |
| `GET` | `/oauth2/authorization/google` | Public | Initiates Google OAuth 2.0 authorization redirect |
| `GET` | `/login/oauth2/code/google` | Public | OAuth 2.0 callback handled by Spring Security |

---

## Prerequisites
Before running the application, make sure you have the following installed on your system:
- **Java 17 or higher** (`java -version`)
- **Apache Maven 3.8+** (`mvn -v`)
- **Node.js 18+ and npm** (`node -v`, `npm -v`)
- **PostgreSQL 14+** (running on port `5432`)

---

## Step 1: Database Setup (PostgreSQL)

1. Open your PostgreSQL terminal (`psql`) or pgAdmin.
2. Create the database:
   ```sql
   CREATE DATABASE nutriscan_db;
   ```
3. Run the schema script (`database/schema.sql`) or let Spring Boot automatically create it on startup:
   ```sql
   CREATE TABLE IF NOT EXISTS users (
       id BIGSERIAL PRIMARY KEY,
       full_name VARCHAR(100) NOT NULL,
       email VARCHAR(150) UNIQUE NOT NULL,
       password_hash VARCHAR(255) NULL,
       role VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
       provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
       created_at TIMESTAMP NOT NULL DEFAULT NOW()
   );

   CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
   ```

---

## Step 2: Configuration Settings

Edit `backend/src/main/resources/application.properties` to match your environment:

### 1. PostgreSQL Credentials
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/nutriscan_db
spring.datasource.username=postgres
spring.datasource.password=your_postgres_password
```

### 2. Google OAuth 2.0 Credentials
To enable Google Sign-In:
1. Visit the [Google Cloud Console](https://console.cloud.google.com/).
2. Create a new project (e.g. `NutriScan`).
3. Navigate to **APIs & Services** > **Credentials**.
4. Click **Create Credentials** > **OAuth client ID**.
5. Select application type: **Web application**.
6. Under **Authorized redirect URIs**, add:
   ```
   http://localhost:8080/login/oauth2/code/google
   ```
7. Copy the **Client ID** and **Client Secret** and update `backend/src/main/resources/application.properties` (or set environment variables `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`):
   ```properties
   spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
   spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET
   ```
*(Note: If you do not have Google credentials yet, local email/password registration and login will work immediately with the default placeholders!)*

---

## Step 3: Running the Application

### 1. Start the Spring Boot Backend
Open a terminal in the `backend/` directory:
```bash
cd backend
mvn spring-boot:run
```
The backend starts on `http://localhost:8080`.

### 2. Start the React Frontend
Open a second terminal in the `frontend/` directory:
```bash
cd frontend
npm install
npm run dev
```
The frontend opens on `http://localhost:5173`.

---

## BCrypt Password Hashing Proof (Database Verification)

When a student registers locally, their plaintext password is never stored. Spring Security's `BCryptPasswordEncoder` produces a 60-character salted hash starting with `$2a$10$` or `$2b$10$`.

You can verify this in PostgreSQL by querying the table:
```sql
SELECT id, full_name, email, password_hash, role, provider, created_at FROM users;
```

**Sample Output for Verification:**
```
 id |   full_name   |          email          |                        password_hash                         |  role   | provider |         created_at         
----+---------------+-------------------------+--------------------------------------------------------------+---------+----------+----------------------------
  1 | Maria Santos  | maria@university.edu.ph | $2a$10$wE3eCqYI8V6J9V8W6x9Xre8mR7qS1L2v9K0Zp5Q3T4M6N8O1P2Q3 | STUDENT | LOCAL    | 2026-11-19 18:30:15.123456
  2 | Juan DelaCruz | juan@gmail.com          | [NULL]                                                       | STUDENT | GOOGLE   | 2026-11-19 18:32:44.789101
```
*(Notice that Google OAuth users have `password_hash = NULL` and `provider = GOOGLE`, while local accounts have a salted 60-character hash).*

---

## How to Demo Increment 1

Follow these exact steps to demonstrate all required features:

### 1. Register a New Student
1. Open your browser and navigate to `http://localhost:5173`.
2. You will be automatically redirected to `http://localhost:5173/login`.
3. Click **"Create a student account"** (or go to `/register`).
4. Fill in the student registration form:
   - **Full Name:** `Maria Santos`
   - **Email:** `maria@university.edu.ph`
   - **Password:** `Password123!`
   - **Confirm Password:** `Password123!`
5. Click **"Register"**. You will be redirected to `/login` with a green success message: *"Registration successful! Please log in with your credentials."*

### 2. Login with That Account
1. On `http://localhost:5173/login`, enter:
   - **Email:** `maria@university.edu.ph`
   - **Password:** `Password123!`
2. Click **"Sign In"**.

### 3. Land on Dashboard
1. You are redirected to `http://localhost:5173/dashboard`.
2. Notice the dashboard displays:
   - Welcome banner: *"Mabuhay, Maria Santos! 👋"*
   - User email and badges: `ROLE: STUDENT` and `Local Email Account`.
   - Navigation shortcuts with future modules marked as *"Next Increment"*.
   - Two placeholder cards:
     - **Daily Calorie Target** (*"Coming in next increment"*)
     - **7-Day History** (*"Coming in next increment"*)

### 4. Try Accessing `/dashboard` Without Login (Redirected)
1. Open an Incognito / Private browser window.
2. Directly type `http://localhost:5173/dashboard` in the address bar.
3. Observe that you are immediately redirected back to `http://localhost:5173/login`.
4. Try accessing the backend endpoint directly via curl or browser:
   ```bash
   curl -i http://localhost:8080/api/me
   ```
   Returns `HTTP/1.1 401 Unauthorized` with JSON:
   ```json
   {"error":"Unauthorized: Please log in to access this resource"}
   ```

### 5. Logout
1. In your logged-in browser session, click the **"Logout"** button in the top navigation bar or the **"Sign Out"** button on the dashboard.
2. The HTTP session is invalidated on the backend and you are redirected to `/login`.
3. Pressing the browser's "Back" button will NOT return to the dashboard because the session is destroyed.

### 6. Login with Google OAuth 2.0
1. On `http://localhost:5173/login`, click **"Continue with Google"**.
2. Spring Boot routes you to Google's consent screen.
3. Sign in with your Google account.
4. Google redirects back to `http://localhost:8080/login/oauth2/code/google`.
5. The backend automatically creates a student record in the PostgreSQL database with:
   - `role = "STUDENT"`
   - `provider = "GOOGLE"`
   - `password_hash = NULL`
6. You are redirected to `http://localhost:5173/dashboard`, showing your Google profile name and a `Google Account` badge.

---

## Automated Tests
Unit and integration tests for password hashing, duplicate accounts, and OAuth logic run using an in-memory test database:
```bash
cd backend
mvn test
```
All tests will execute and pass without requiring an external PostgreSQL instance to be active.
