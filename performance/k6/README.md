# Admissions simulation

The default scenario submits 100 applications at a rate of one application
every three minutes. The complete run lasts approximately five hours.

Prerequisites:

- EduCue is running on `http://localhost:8080` (or provide `BASE_URL`).
- At least one intake is open, its application deadline has not passed, and it
  has at least one attached course.
- [k6](https://grafana.com/docs/k6/latest/set-up/install-k6/) is installed.

Run the requested simulation from the backend project root:

```powershell
k6 run performance/k6/admissions.js
```

Use a different backend URL:

```powershell
$env:BASE_URL = "https://api.example.test"
k6 run performance/k6/admissions.js
```

For a short smoke test before the five-hour run:

```powershell
$env:APPLICATION_COUNT = "5"
$env:INTERVAL_SECONDS = "2"
k6 run performance/k6/admissions.js
```

Clear those overrides before the real run:

```powershell
Remove-Item Env:APPLICATION_COUNT -ErrorAction SilentlyContinue
Remove-Item Env:INTERVAL_SECONDS -ErrorAction SilentlyContinue
```

The script discovers valid intake-course IDs from `/api/intakes/open`, rotates
through all returned courses, and creates unique emails and national IDs. It
does not upload documents or approve applications.
