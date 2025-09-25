
# Vibe Jobs Aggregator — Greenhouse Example Included

Adds a real careers API connector: **Greenhouse** (`GreenhouseSourceClient`).

## Run
```
mvn spring-boot:run
```
Scheduler starts 10s after boot and pulls from:
- MockCareersApiSource("Acme")
- GreenhouseSourceClient("stripe")

Replace `"stripe"` with other Greenhouse board slugs when needed.

## Notes
- The example endpoint `GET https://boards-api.greenhouse.io/v1/boards/{company}/jobs?content=true` does not provide a posted time; we set `postedAt=now()`. You can enrich by calling the job detail endpoint for `updated_at` if needed.
- Results are upserted by `(source, externalId)` and exposed via `GET /jobs` to match your Next.js frontend.
