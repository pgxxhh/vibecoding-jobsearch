# Aurora/RDS Production Database Checklist

Use this checklist when deploying the backend to AWS ECS with an Aurora MySQL or
RDS MySQL cluster. The goal is to ensure the service can authenticate against
the managed database and that Flyway bootstraps the schema before traffic
arrives.

## 1. Networking and access
- [ ] Place the ECS service in the same VPC/subnets as the database or ensure
      peering is configured.
- [ ] Add inbound rules to the database security group allowing port `3306`
      from the ECS task security group only (keep the instance private to the
      internet).
- [ ] Confirm the database endpoint resolves from the ECS tasks (e.g. via
      `nslookup database-vibejobs.clgia4qkyyuz.ap-southeast-1.rds.amazonaws.com`).

## 2. Prepare application environment variables
Create a `.env.production` file beside `deploy.sh` or configure the equivalent
variables in the ECS task definition/Secrets Manager:

| Variable | Example value | Notes |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `mysql,prod` | Enables the MySQL profile and production overrides. |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://database-vibejobs.clgia4qkyyuz.ap-southeast-1.rds.amazonaws.com:3306/vibejobs?useSSL=true&requireSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC` | Replace host/port if your cluster differs. |
| `SPRING_DATASOURCE_USERNAME` | `vibejobs` | Aurora user with DDL/DML permissions. |
| `SPRING_DATASOURCE_PASSWORD` | `********` | Store in Secrets Manager or SSM Parameter Store. |
| `SPRING_DATASOURCE_DRIVER_CLASS_NAME` | `com.mysql.cj.jdbc.Driver` | Ensures the correct JDBC driver is used. |
| `SPRING_JPA_DATABASE_PLATFORM` | `org.hibernate.dialect.MySQLDialect` | Keeps Hibernate aligned with MySQL syntax. |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `validate` | Prevents Hibernate from altering schema; Flyway owns migrations. |
| `SPRING_FLYWAY_SCHEMAS` | `vibejobs` | Matches the database name created in RDS. |

If you copy `.env.production.example`, make sure to update the password and any
database-specific overrides before deploying.

## 3. Run Flyway migrations once
- [ ] Ensure the `vibejobs` schema exists: `CREATE DATABASE vibejobs CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`
- [ ] Trigger a backend deployment (`./deploy.sh` or an ECS force deployment).
- [ ] Watch the backend logs; Flyway should emit `Successfully applied 2 migrations`.
- [ ] Verify tables with `mysql -h <endpoint> -u vibejobs -p -e 'SHOW TABLES IN vibejobs;'`.

## 4. Post-deployment verification
- [ ] Hit `https://<your-domain>/api/actuator/health` and confirm status `UP`.
- [ ] Run a manual ingestion (`POST /api/jobs/refresh`) or wait for the
      scheduler and confirm new rows appear in `jobs`/`job_details`.
- [ ] Enable CloudWatch alarms on ECS task failures and RDS CPU/storage metrics.

Following this checklist keeps production aligned with the validated local
setup while letting Flyway manage schema changes safely.
