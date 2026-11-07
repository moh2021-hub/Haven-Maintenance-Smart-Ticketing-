# Apartment Maintenance Request Prioritizer — Spring Boot API

Ranks maintenance requests so staff always know what to fix first:

```
GAS_LEAK (1) > NO_HEAT (2) > WATER_LEAK (3) > BROKEN_APPLIANCE (4) > OTHER (5)
```

Plus technician assignment, status tracking, photo upload and an estimated completion time.

---

## 1. Run it locally (30 seconds, no AWS, no MySQL)

```bash
cd maintenance-api
mvn spring-boot:run
```

The default profile uses an **H2 in-memory database**, so nothing has to be installed.
On startup it seeds 3 technicians and 4 requests so the API is never empty.

- API: <http://localhost:8080/api/requests>
- DB browser: <http://localhost:8080/h2-console> (JDBC URL `jdbc:h2:mem:maintenancedb`, user `sa`, no password)

Build a jar instead:

```bash
mvn clean package
java -jar target/application.jar
```

---

## 2. Endpoints

### Requests

| Method | URL | What it does |
|---|---|---|
| POST | `/api/requests` | Create a request. Priority + ETA are set automatically. |
| GET | `/api/requests` | **The queue** — most urgent first, oldest first within a tie. |
| GET | `/api/requests?status=NEW` | Same, filtered by status. |
| GET | `/api/requests/summary` | Counts per status, for a dashboard. |
| GET | `/api/requests/{id}` | One request. |
| PUT | `/api/requests/{id}/assign?technicianId=2` | Assign a technician (status → IN_PROGRESS). |
| PUT | `/api/requests/{id}/auto-assign` | Give it to whoever has the fewest open jobs. |
| PUT | `/api/requests/{id}/status?status=COMPLETED` | Update status. |
| POST | `/api/requests/{id}/photo` | Upload a photo (multipart field `file`). |
| DELETE | `/api/requests/{id}` | Delete. |

`requestType`: `GAS_LEAK`, `NO_HEAT`, `WATER_LEAK`, `BROKEN_APPLIANCE`, `OTHER`
`status`: `NEW`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`

### Technicians

| Method | URL | What it does |
|---|---|---|
| GET | `/api/technicians` | List all. |
| GET | `/api/technicians/{id}` | One technician. |
| GET | `/api/technicians/{id}/requests` | That technician's jobs, urgent first. |
| POST | `/api/technicians` | Create one. |
| DELETE | `/api/technicians/{id}` | Delete (their open jobs go back to NEW). |

---

## 3. Try it with curl

```bash
# Resident reports a gas smell in 302
curl -X POST http://localhost:8080/api/requests \
  -H "Content-Type: application/json" \
  -d '{"apartmentNumber":"302","description":"Gas smell in the kitchen","requestType":"GAS_LEAK"}'
```

Response — note `priority: 1` and the 2-hour ETA, both filled in by the backend:

```json
{
  "id": 5,
  "apartmentNumber": "302",
  "description": "Gas smell in the kitchen",
  "requestType": "GAS_LEAK",
  "priority": 1,
  "status": "NEW",
  "technician": null,
  "estimatedCompletionTime": "2026-11-06T16:30:12Z",
  "photoUrl": null,
  "createdAt": "2026-11-06T14:30:12Z",
  "completedAt": null,
  "overdue": false
}
```

Timestamps are `java.time.Instant`, so they always carry the `Z` and always mean UTC.
That matters: Elastic Beanstalk runs in UTC while your browser does not, and a bare
`LocalDateTime` has no zone for the browser to read, so every ETA would silently land
hours off. The React app converts to the viewer's local time on its own.

```bash
# The prioritized queue
curl http://localhost:8080/api/requests

# Assign technician 1, then auto-assign another one
curl -X PUT "http://localhost:8080/api/requests/5/assign?technicianId=1"
curl -X PUT "http://localhost:8080/api/requests/2/auto-assign"

# Technician finishes the job
curl -X PUT "http://localhost:8080/api/requests/5/status?status=COMPLETED"

# Attach a photo
curl -X POST http://localhost:8080/api/requests/5/photo -F "file=@leak.jpg"

# Add a technician
curl -X POST http://localhost:8080/api/technicians \
  -H "Content-Type: application/json" \
  -d '{"name":"John Carter","phone":"313-555-0101"}'
```

---

## 4. How the priority logic works

All of it is in `model/RequestType.java`:

```java
GAS_LEAK(1, 2),          // priority 1, fix within 2 hours
NO_HEAT(2, 8),
WATER_LEAK(3, 24),
BROKEN_APPLIANCE(4, 72),
OTHER(5, 120);
```

On create, `MaintenanceService` copies the priority onto the row and sets
`estimatedCompletionTime = now + hoursToFix`. The repository sorts with
`ORDER BY priority ASC, createdAt ASC`, so the queue is always correct.
To change the ranking, edit those five numbers — nothing else.

---

## 5. Calling it from React

```javascript
const API = "http://localhost:8080/api";

// Get the prioritized queue
const res = await fetch(`${API}/requests`);
const requests = await res.json();

// Create a request
await fetch(`${API}/requests`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({
    apartmentNumber: "302",
    description: "Gas smell in the kitchen",
    requestType: "GAS_LEAK",
  }),
});

// Upload a photo
const form = new FormData();
form.append("file", fileInput.files[0]);
await fetch(`${API}/requests/5/photo`, { method: "POST", body: form });
```

CORS is already open for `localhost:3000` and `localhost:5173` in `config/WebConfig.java`.

---

## 6. Deploying to AWS

Switch to the `prod` profile and everything points at RDS + S3 instead of H2 + local disk.

**RDS MySQL** — create a `db.t3.micro` MySQL instance, and in the Elastic Beanstalk
console set these environment properties:

```
SPRING_PROFILES_ACTIVE = prod
RDS_HOSTNAME           = your-db.abc123.us-east-1.rds.amazonaws.com
RDS_PORT               = 3306
RDS_DB_NAME            = maintenancedb
RDS_USERNAME           = admin
RDS_PASSWORD           = your-password
S3_BUCKET              = your-bucket-name
AWS_REGION             = us-east-1
```

Hibernate creates the tables on first boot (`ddl-auto=update`).

**Elastic Beanstalk** — create a *Java SE* environment, then:

```bash
mvn clean package
# upload target/application.jar
```

The `Procfile` in the project root tells Beanstalk how to start it on port 5000.
Make sure the RDS security group allows inbound MySQL from the Beanstalk security group.

**S3** — create a bucket, and attach a policy allowing `s3:PutObject` to the
`aws-elasticbeanstalk-ec2-role`. No access keys go in the code: the SDK picks up
the instance role automatically.

---

## 7. Project layout

```
src/main/java/com/apartment/maintenance/
├── MaintenanceApplication.java        main()
├── model/
│   ├── MaintenanceRequest.java        the main table
│   ├── Technician.java
│   ├── RequestType.java               <-- priority + ETA rules live here
│   └── RequestStatus.java
├── repository/                        Spring Data JPA, sorted queries
├── dto/                               what React sends in
├── service/
│   ├── MaintenanceService.java        priority, assignment, status
│   └── PhotoStorageService.java       S3 or local disk
├── controller/                        REST endpoints
├── config/                            CORS, static photos, seed data
└── exception/                         clean JSON errors
```
