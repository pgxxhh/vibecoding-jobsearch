#!/usr/bin/env bash
set -euo pipefail

# Database setup script - run this once to set up your production database

echo "=== Production Database Setup ==="

# Database connection details
DB_HOST=${DB_HOST:-"database-vibejobs.clgia4qkyyuz.ap-southeast-1.rds.amazonaws.com"}
DB_PORT=${DB_PORT:-"3306"}
DB_NAME=${DB_NAME:-"vibejobs"}
DB_USER=${DB_USER}
DB_PASSWORD=${DB_PASSWORD}

if [ -z "$DB_USER" ] || [ -z "$DB_PASSWORD" ]; then
    echo "Error: Please set DB_USER and DB_PASSWORD environment variables"
    echo "Example:"
    echo "export DB_USER=your_username"
    echo "export DB_PASSWORD=your_password"
    exit 1
fi

echo "Connecting to: $DB_HOST:$DB_PORT/$DB_NAME"
echo "User: $DB_USER"
echo

echo ">> Setting up database schema..."
SCHEMA_SCRIPTS=(
    "vibe-jobs-aggregator/scripts/simple-db-setup.sql"
    "vibe-jobs-aggregator/scripts/2024-07-15_admin_tables.sql"
)

for schema_script in "${SCHEMA_SCRIPTS[@]}"; do
    if [[ ! -f "$schema_script" ]]; then
        echo "Error: missing schema script $schema_script" >&2
        exit 1
    fi
    echo "   -> running ${schema_script##*/}"
    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < "$schema_script"
done

echo
echo ">> Verifying setup..."
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -e "
SELECT TABLE_NAME FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = '$DB_NAME' 
ORDER BY TABLE_NAME;
"

echo
echo "=== Database setup completed! ==="
echo "You can now run: sh deploy.sh"