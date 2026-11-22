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

## Demo video

`scripts/demo-video/record-demo.mjs` records a captioned walkthrough of the demo (about 1.5 minutes, 1280×800, WebM):
the client signing in and browsing Filings, Financials, Computations and Misc by financial year, a download, then the
admin dashboard, the PAN dropdown on Upload Documents, and filtering All Documents.

1. Start the app with the demo profile on a fresh demo database (see **Demo** above).
2. Record (needs Node.js; installs Playwright and its Chromium on first use):
   ```bash
   cd scripts/demo-video
   npm install --no-save playwright && npx playwright install chromium
   BASE=http://localhost:8080 node record-demo.mjs ../../demo-video
   ```

The video is written to `demo-video/docushare-demo.webm` (not committed). WebM plays in Chrome, Firefox, Edge and VLC;
for QuickTime or PowerPoint convert it with ffmpeg: `ffmpeg -i docushare-demo.webm -c:v libx264 -pix_fmt yuv420p docushare-demo.mp4`.

## Docker

To build a Docker image, run:

```bash
docker build -t my-application:latest .
```

If you use commercial components, pass the license key as a build secret:

```bash
docker build --secret id=proKey,src=$HOME/.vaadin/proKey .
```
