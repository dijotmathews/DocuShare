# Development

## Build & Run Commands

```bash
./mvnw                            # Run in dev mode (default goal: spring-boot:run)
./mvnw clean package              # Production build (JAR in target/)
./mvnw test                       # Run all tests
./mvnw test -Dtest=ClassName      # Run a single test class
```

The app runs on port 8080 (configurable via `PORT` env var).

## Demo

Start the app with the `demo` profile to get a ready-made client to show:

```bash
DB_PATH=demo.db DOCUMENTS_ROOT=demo-documents ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

On first start this creates **Demo Traders Pvt Ltd** (PAN `DEMOC1234D`) with one document of every type for each of
the last three completed financial years: 21 small PDFs, so Filings, Financials, Computations and Misc each have
documents in three financial years. Upload dates are backdated to when each kind of document is typically uploaded,
in the year after its financial year: Misc in June, Balance Sheet and Profit & Loss in mid August, Audit Report in
early September, Tax Audit Report in late September, Tax Computation in mid October and IT Acknowledgement in late
October. Dates that would still be in the future (for a financial year that ended recently) become the last few hours
before startup instead, in the same order. Starting again with the same database adds nothing; to refresh the demo
(for example to get the new dates), delete the demo database and documents folder first.

| Sign in as | Where | Username | Password |
|------------|-------|----------|----------|
| Client | `/login` | `DEMOC1234D` | `DEMOC1234D@01042015` |
| Admin | `/admin/login` | `admin` | `admin` (or `ADMIN_PASSWORD`) |

Use a separate `DB_PATH` and `DOCUMENTS_ROOT` (as above) to keep demo data out of your normal database. Without the
profile nothing is created.

## Docker

To build a Docker image, run:

```bash
docker build -t my-application:latest .
```

If you use commercial components, pass the license key as a build secret:

```bash
docker build --secret id=proKey,src=$HOME/.vaadin/proKey .
```
