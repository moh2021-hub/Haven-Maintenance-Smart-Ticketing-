# Apartment Maintenance Request Prioritizer

Apartment staff get maintenance requests faster than they can work them, and the
dangerous ones arrive mixed in with the annoying ones. This ranks them so the gas
leak is always at the top of the board, even when it was reported last.

```
GAS_LEAK (1) > NO_HEAT (2) > WATER_LEAK (3) > BROKEN_APPLIANCE (4) > OTHER (5)
```

Plus technician assignment, status tracking, photo upload, and an estimated
completion time per request.

```
React (Vite)  ──REST──>  Spring Boot  ──>  MySQL on AWS RDS
                              │
                              └──>  photos on AWS S3
                         (hosted on Elastic Beanstalk)
```

---

## Run the whole thing locally

Two terminals. No MySQL, no AWS account, no Docker.

**Terminal 1 — backend**

```bash
cd backend
mvn spring-boot:run
```

Starts on <http://localhost:8080> with an in-memory H2 database, and seeds three
technicians and four requests so the board is never empty.

**Terminal 2 — frontend**

```bash
cd frontend
npm install
npm run dev
```

Open <http://localhost:3000>.

That is it. CORS for ports 3000 and 5173 is already configured on the backend.

---

## What you get

The board is sorted by urgency, not by arrival time. Each row carries a colored
spine and its rank, so staff can read the queue from across a room. The colors are
the utility locate-marking codes already used on job sites — yellow for gas, red
for heat and power, blue for water, green for drains.

Things you can do from the board:

- Log a request; priority and the deadline are assigned by the backend
- Assign a technician by name, or let the backend pick whoever has the fewest open jobs
- Mark a job done, or reopen it
- Attach a photo to a request
- Filter to waiting / in progress / done
- Add and remove technicians, with each one's open job count

Requests past their estimated completion time are flagged in red, in the row and in
the header count.

---

## Layout

```
apartment-maintenance/
├── backend/          Spring Boot API  — see backend/README.md for endpoints and AWS deploy
│   └── src/main/java/com/apartment/maintenance/
│       ├── model/RequestType.java       <-- the priority rules, all five of them
│       ├── service/MaintenanceService.java
│       ├── service/PhotoStorageService.java   S3, or local disk when S3 is off
│       ├── controller/                  REST endpoints
│       └── repository/                  sorted JPA queries
└── frontend/         React + Vite
    └── src/
        ├── api.js                       every backend call, in one file
        ├── labels.js                    enum names -> plain English, deadline math
        ├── App.jsx
        └── components/
            ├── BoardHeader.jsx          what to handle first
            ├── RequestRow.jsx           one request, with all its actions
            ├── NewRequestForm.jsx
            └── TechnicianPanel.jsx
```

---

## Changing the ranking

Everything lives in one enum, `backend/.../model/RequestType.java`:

```java
GAS_LEAK(1, 2),          // priority 1, due within 2 hours
NO_HEAT(2, 8),
WATER_LEAK(3, 24),
BROKEN_APPLIANCE(4, 72),
OTHER(5, 120);
```

The service stamps that priority onto the row at creation time and the repository
sorts `ORDER BY priority ASC, createdAt ASC`. Two gas leaks? The one reported first
goes first. Add a category, or reorder the numbers, and both the API and the board
follow — the frontend reads the priority number off each request rather than keeping
its own copy of the rules.

---

## Going to AWS

`backend/README.md` has the full walkthrough: RDS MySQL, S3, and Elastic Beanstalk
with the environment properties to set. The short version is that the `prod` profile
swaps H2 for RDS and local disk for S3, and no code changes.

For the frontend, point it at the deployed API and build:

```bash
cd frontend
echo "VITE_API_URL=https://your-env.elasticbeanstalk.com/api" > .env
npm run build          # static files land in dist/
```

Then either host `dist/` on S3 as a static site, or drop its contents into the
backend's `src/main/resources/static/` before packaging the jar so Spring Boot serves
both. If you host the frontend on its own domain, add that domain to the
`allowedOrigins` list in `backend/.../config/WebConfig.java`.

---

## A note on timestamps

The API sends timestamps as UTC instants ending in `Z`. This is deliberate. Elastic
Beanstalk runs in UTC and your browser does not, and Java's `LocalDateTime`
serializes with no timezone at all, so the browser would read the server's wall clock
as its own and every deadline would be off by your UTC offset. Using `Instant` makes
the conversion the browser's job, which is where it belongs.
