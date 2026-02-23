# Spring Security Implementation - Detailed Documentation

---

## Table of Contents

1. [Overview](#overview)
2. [Technology Stack](#technology-stack)
3. [Architecture Diagram](#architecture-diagram)
4. [Core Security Components](#core-security-components)
   - [SecurityConfig](#1-securityconfig)
   - [MyUserDetailService](#2-myuserdetailservice)
   - [MyUserDetails](#3-myuserdetails)
   - [DaoAuthenticationProvider](#4-daoauthenticationprovider)
5. [Data Model](#data-model)
   - [User Entity](#user-entity)
   - [Role Entity](#role-entity)
   - [Database Schema](#database-schema)
6. [Authentication Flow](#authentication-flow)
7. [Authorization Model](#authorization-model)
   - [URL-Level Authorization](#url-level-authorization)
   - [Method-Level Authorization](#method-level-authorization)
8. [Password Security](#password-security)
9. [Session Management](#session-management)
10. [CSRF Protection](#csrf-protection)
11. [API Endpoints Reference](#api-endpoints-reference)
12. [User Registration Flow](#user-registration-flow)
13. [Admin Promotion Flow](#admin-promotion-flow)
14. [Key Design Decisions](#key-design-decisions)
15. [Security Considerations & Limitations](#security-considerations--limitations)

---

## Overview

This project implements a **database-backed, stateless REST API** secured with Spring Security. It uses:

- **HTTP Basic Authentication** for credential transmission
- **BCrypt** for password hashing
- **Role-Based Access Control (RBAC)** for authorization
- **MySQL** as the persistent user/role store
- **Method-level security** via `@PreAuthorize` annotations
- **Stateless sessions** — no server-side HTTP session is created

---

## Technology Stack

| Component            | Technology                        |
|----------------------|-----------------------------------|
| Security Framework   | Spring Security 6.x               |
| Authentication Type  | HTTP Basic                        |
| Password Encoder     | BCrypt (strength 10)              |
| Database             | MySQL                             |
| ORM                  | Spring Data JPA / Hibernate       |
| Session Strategy     | Stateless (no sessions)           |
| Authorization        | RBAC with `@PreAuthorize`         |

---

## Architecture Diagram

```
HTTP Request
     │
     ▼
┌─────────────────────────────────┐
│     Spring Security Filter Chain │
│                                 │
│  ┌──────────────────────────┐   │
│  │  BasicAuthenticationFilter│   │  ◄── Reads "Authorization: Basic ..." header
│  └────────────┬─────────────┘   │
│               │                 │
│  ┌────────────▼─────────────┐   │
│  │ DaoAuthenticationProvider │   │  ◄── Validates credentials
│  └────────────┬─────────────┘   │
│               │                 │
│  ┌────────────▼─────────────┐   │
│  │   MyUserDetailService    │   │  ◄── Loads user from MySQL
│  └────────────┬─────────────┘   │
│               │                 │
│  ┌────────────▼─────────────┐   │
│  │   BCryptPasswordEncoder  │   │  ◄── Verifies hashed password
│  └────────────┬─────────────┘   │
│               │                 │
│  ┌────────────▼─────────────┐   │
│  │  Authorization Check     │   │  ◄── URL rules + @PreAuthorize
│  └────────────┬─────────────┘   │
└───────────────┼─────────────────┘
                │
                ▼
         Controller Method
```

---

## Core Security Components

### 1. SecurityConfig

**File:** `src/main/java/.../config/SecurityConfig.java`

This is the central security configuration class, annotated with:

- `@Configuration` — marks it as a Spring configuration class
- `@EnableWebSecurity` — activates Spring Security's web security support
- `@EnableMethodSecurity` — enables method-level security annotations like `@PreAuthorize`

#### AuthenticationProvider Bean

```java
@Bean
public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(new BCryptPasswordEncoder(10));
    return provider;
}
```

- Uses `DaoAuthenticationProvider` — the standard Spring Security provider for database-backed authentication.
- Wires in the custom `MyUserDetailService` to load user details from MySQL.
- Sets `BCryptPasswordEncoder` with **cost factor 10** to verify passwords.

#### SecurityFilterChain Bean

```java
@Bean
SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests((requests) -> requests
        .requestMatchers("/api/addUser").permitAll()
        .requestMatchers("/api/hello").permitAll()
        .anyRequest().authenticated()
    );

    http.sessionManagement(session ->
        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.csrf(csrf -> csrf.disable());
    http.httpBasic(Customizer.withDefaults());
    return http.build();
}
```

This defines:
- Which endpoints are public vs. protected (see [URL-Level Authorization](#url-level-authorization))
- Stateless session policy
- CSRF disabled
- HTTP Basic as the authentication mechanism

#### Commented-Out In-Memory Users

A commented-out block shows an earlier approach using `InMemoryUserDetailsManager` with `{noop}` (no encoding). This was **replaced** by the database-backed approach — a significant security improvement.

---

### 2. MyUserDetailService

**File:** `src/main/java/.../service/MyUserDetailService.java`

Implements Spring Security's `UserDetailsService` interface. Its sole job is to load a user from the database by username.

```java
@Override
public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user = repository.findByUsername(username);
    if (user == null) throw new UsernameNotFoundException("Username not found");
    return new MyUserDetails(user);
}
```

- Called by `DaoAuthenticationProvider` during every login attempt.
- Throws `UsernameNotFoundException` if the user doesn't exist, which Spring Security maps to a `401 Unauthorized` response.
- Wraps the JPA `User` entity in a `MyUserDetails` adapter.

---

### 3. MyUserDetails

**File:** `src/main/java/.../entity/MyUserDetails.java`

Adapts the JPA `User` entity to implement Spring Security's `UserDetails` interface.

```java
@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    return user.getRoles().stream()
        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRole()))
        .toList();
}
```

**Critical detail:** Each role name is prefixed with `ROLE_`. This is the Spring Security convention.  
So a role stored as `"ADMIN"` in the database becomes `"ROLE_ADMIN"` as a `GrantedAuthority`.  
This is why `@PreAuthorize("hasRole('ADMIN')")` works — Spring internally checks for `ROLE_ADMIN`.

| DB Role Value | GrantedAuthority        | `hasRole()` check       |
|---------------|-------------------------|-------------------------|
| `USER`        | `ROLE_USER`             | `hasRole('USER')`       |
| `ADMIN`       | `ROLE_ADMIN`            | `hasRole('ADMIN')`      |

---

### 4. DaoAuthenticationProvider

This is a built-in Spring Security class configured in `SecurityConfig`. It:

1. Calls `MyUserDetailService.loadUserByUsername()` to fetch the user
2. Uses `BCryptPasswordEncoder` to compare the incoming plaintext password against the stored hash
3. Throws `BadCredentialsException` if passwords don't match → results in `401 Unauthorized`

---

## Data Model

### User Entity

**File:** `src/main/java/.../entity/User.java`

```
users table
┌────────────┬──────────────┬──────────────────────────────────┐
│ id (PK)    │ username     │ password                         │
│ INT AUTO   │ VARCHAR      │ VARCHAR (BCrypt hash)            │
└────────────┴──────────────┴──────────────────────────────────┘
```

- Passwords are **never stored in plaintext** — always BCrypt-hashed.
- Has a `ManyToMany` relationship with `Role` via the `user_roles` join table.
- Roles are fetched **eagerly** (`FetchType.EAGER`) so they are always available during authentication.

### Role Entity

**File:** `src/main/java/.../entity/Role.java`

```
role table
┌────────────┬──────────────┐
│ id (PK)    │ role         │
│ INT AUTO   │ VARCHAR      │
└────────────┴──────────────┘
```

- `@JsonIgnore` on the `users` collection prevents circular serialization when roles are returned in API responses.

### Database Schema

```
users                user_roles              role
┌─────────────┐     ┌──────────────────┐    ┌─────────────┐
│ id (PK)     │────<│ user_id (FK)     │    │ id (PK)     │
│ username    │     │ role_id (FK)     │>───│ role        │
│ password    │     └──────────────────┘    └─────────────┘
└─────────────┘
```

A single user can have **multiple roles** (e.g., both USER and ADMIN). A single role can belong to **multiple users**.

---

## Authentication Flow

This is the step-by-step sequence for every protected API request:

```
1. Client sends:
   GET /api/user
   Authorization: Basic base64(username:password)

2. BasicAuthenticationFilter decodes the Base64 header
   → extracts username and plaintext password

3. DaoAuthenticationProvider calls:
   MyUserDetailService.loadUserByUsername("username")
   → queries MySQL: SELECT * FROM users WHERE username = ?
   → wraps result in MyUserDetails

4. BCryptPasswordEncoder.matches(plaintext, storedHash)
   → if false → 401 Unauthorized
   → if true  → authentication succeeds

5. SecurityContext is populated with the authenticated principal
   (username + granted authorities like ROLE_USER, ROLE_ADMIN)

6. Authorization check:
   → URL rule: is this endpoint open or requires authentication?
   → @PreAuthorize: does the user have the required role?
   → if fails → 403 Forbidden
   → if passes → request reaches the Controller
```

---

## Authorization Model

### URL-Level Authorization

Defined in `SecurityConfig.defaultSecurityFilterChain()`:

| Endpoint          | Access Rule          | Reason                              |
|-------------------|----------------------|-------------------------------------|
| `POST /api/addUser` | `permitAll()`      | Public registration — no login needed |
| `GET /api/hello`  | `permitAll()`        | Public greeting endpoint            |
| All other URLs    | `authenticated()`    | Any logged-in user, regardless of role |

### Method-Level Authorization

Enabled by `@EnableMethodSecurity` on `SecurityConfig`. Uses `@PreAuthorize` annotations directly on controller methods.

| Endpoint              | Annotation                         | Required Role |
|-----------------------|------------------------------------|---------------|
| `GET /api/user`       | `@PreAuthorize("hasRole('USER')")` | USER          |
| `GET /api/admin`      | `@PreAuthorize("hasRole('ADMIN')")` | ADMIN        |
| `POST /api/admin/{id}`| `@PreAuthorize("hasRole('ADMIN')")` | ADMIN        |

**How `@PreAuthorize` works:**  
Spring Security evaluates the SpEL (Spring Expression Language) expression before the method executes. If the currently authenticated user does not have the required `GrantedAuthority`, a `403 Forbidden` is returned immediately — the controller code never runs.

---

## Password Security

| Aspect              | Detail                                      |
|---------------------|---------------------------------------------|
| Algorithm           | BCrypt                                      |
| Cost Factor         | 10 (2^10 = 1,024 iterations)               |
| Salt                | Automatically generated per-password by BCrypt |
| Storage             | Hashed value only — plaintext never stored  |
| Encoding location   | `UserService.saveUser()` on registration    |

```java
// In UserService:
private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

public User saveUser(UserDAO userDAO) {
    user.setPassword(encoder.encode(userDAO.getPassword())); // hashed before DB insert
    ...
}
```

BCrypt at cost 10 is computationally expensive by design, making brute-force and dictionary attacks significantly harder.

---

## Session Management

```java
http.sessionManagement(session ->
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
```

- **`STATELESS`** means Spring Security will **never create an HTTP session** (`HttpSession`).
- Every request must carry credentials in the `Authorization` header.
- This is the standard approach for **stateless REST APIs**.
- No cookies, no session tokens — the server holds zero authentication state between requests.

---

## CSRF Protection

```java
http.csrf(csrf -> csrf.disable());
```

CSRF (Cross-Site Request Forgery) protection is **disabled**.  

**Why this is safe here:**  
CSRF attacks exploit browser cookie-based session authentication. Since this application uses:
- HTTP Basic Authentication (credentials in header, not cookies)
- Stateless sessions (no session cookies are set)

...there is no session cookie for a malicious site to exploit. Disabling CSRF is the correct and standard practice for stateless REST APIs.

---

## API Endpoints Reference

| Method | Endpoint           | Auth Required | Role Required | Description                    |
|--------|--------------------|---------------|---------------|--------------------------------|
| POST   | `/api/addUser`     | No            | None          | Register a new user            |
| GET    | `/api/hello`       | No            | None          | Public hello message           |
| GET    | `/api/user`        | Yes           | USER          | Greeting for USER role         |
| GET    | `/api/admin`       | Yes           | ADMIN         | Greeting for ADMIN role        |
| POST   | `/api/admin/{id}`  | Yes           | ADMIN         | Promote user `{id}` to ADMIN   |

### Example: Calling a Protected Endpoint

```http
GET /api/user HTTP/1.1
Host: localhost:8080
Authorization: Basic dXNlcjpwYXNzd29yZA==
```
Where `dXNlcjpwYXNzd29yZA==` is `Base64("user:password")`.

---

## User Registration Flow

```
POST /api/addUser
Body: { "username": "alice", "password": "secret123" }

1. UserController.createUser() receives UserDAO (username + raw password)
2. UserService.saveUser() is called:
   a. Encodes password:  BCrypt("secret123") → "$2a$10$..."
   b. Finds or creates "USER" role via RoleService
   c. Assigns the USER role to the new user
   d. Saves to MySQL
3. Returns the saved User entity (password hash visible — consider masking in production)
```

New users are always assigned the `USER` role by default. They cannot self-assign `ADMIN`.

---

## Admin Promotion Flow

```
POST /api/admin/5
Authorization: Basic <admin-credentials>

1. Spring Security authenticates the caller
2. @PreAuthorize("hasRole('ADMIN')") checks the caller has ROLE_ADMIN
3. AdminController.makeAdmin(5) is called
4. UserService.makeAdmin(5):
   a. Loads user with id=5 from DB
   b. Finds or creates "ADMIN" role via RoleService
   c. Adds ADMIN role to the user's existing role set
   d. Saves updated user to MySQL
5. Returns updated User entity
```

Only an existing ADMIN can promote another user to ADMIN — enforced by `@PreAuthorize`.

---

## Key Design Decisions

### 1. Database-Backed Authentication (vs. In-Memory)
The commented-out `InMemoryUserDetailsManager` block shows the original approach. The current approach uses MySQL, enabling:
- Persistent users across restarts
- Dynamic user creation via API
- Real-world scalability

### 2. `FetchType.EAGER` for Roles
Roles are loaded eagerly on the `User` entity. This is intentional: during authentication, `getAuthorities()` is called immediately, so roles must be available without a separate lazy-load trigger. In a read-heavy application, consider caching.

### 3. `findOrCreate` Pattern for Roles
`RoleService.findOrCreate("USER")` prevents duplicate role rows being inserted. This is important because roles are shared across users via the join table — each role name should exist exactly once in the `role` table.

### 4. `UserDAO` as a Data Transfer Object
`UserDAO` (username + password) is used as the request body for registration, keeping the JPA `User` entity separate from the API contract. This prevents users from injecting unexpected fields (e.g., setting their own id or roles directly).

### 5. `@JsonIgnore` on `Role.users`
Prevents infinite recursion in JSON serialization (`User → Role → User → ...`).

---

## Security Considerations & Limitations

| Issue | Description | Recommendation |
|-------|-------------|----------------|
| **Password in response** | `POST /api/addUser` returns the `User` entity including the BCrypt hash | Add `@JsonIgnore` on `User.password` or use a response DTO |
| **No HTTPS enforcement** | HTTP Basic sends credentials Base64-encoded (not encrypted) over HTTP | Enforce HTTPS (TLS) in production |
| **No account lockout** | Unlimited failed login attempts are allowed | Add brute-force protection (rate limiting, lockout after N failures) |
| **No token-based auth** | HTTP Basic requires credentials on every request | Consider JWT or OAuth2 for production APIs |
| **DB credentials in properties** | `application.properties` contains plaintext DB password | Use environment variables or a secrets manager in production |
| **No password policy** | Registration accepts any password length/complexity | Add validation (minimum length, complexity rules) |
| **ADMIN self-promotion** | If an ADMIN's credentials are compromised, they can promote any user | Implement audit logging and multi-approval for privilege escalation |

---

*Generated: February 23, 2026*  
*Project: SpringSecurity — Spring Boot + Spring Security 6.x*
