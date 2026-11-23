#!/usr/bin/env python3
"""
Group 10 Car Rental System - Database Initializer
Initializes db/car_rental.db using schema.sql and seed_data.sql via standard library sqlite3.
"""

import os
import sys
import sqlite3
import argparse

def get_script_dir():
    return os.path.dirname(os.path.abspath(__file__))

def init_database(db_file=None, schema_file=None, seed_file=None, reset=False):
    base_dir = get_script_dir()
    
    if db_file is None:
        db_file = os.path.join(base_dir, "car_rental.db")
    if schema_file is None:
        schema_file = os.path.join(base_dir, "schema.sql")
    if seed_file is None:
        seed_file = os.path.join(base_dir, "seed_data.sql")
        
    print("=" * 60)
    print("Group 10 Car Rental System - Database Setup")
    print("=" * 60)
    print(f"Target Database : {db_file}")
    print(f"Schema Script   : {schema_file}")
    print(f"Seed Data Script: {seed_file}")
    print("-" * 60)

    # Reset if requested
    if reset and os.path.exists(db_file):
        try:
            os.remove(db_file)
            print(f"[RESET] Existing database removed: {db_file}")
        except Exception as e:
            print(f"[WARN] Could not remove existing database: {e}")

    # Ensure parent directory exists
    os.makedirs(os.path.dirname(os.path.abspath(db_file)), exist_ok=True)

    # Read SQL files
    if not os.path.exists(schema_file):
        print(f"[ERROR] Schema file not found: {schema_file}", file=sys.stderr)
        return False
    with open(schema_file, "r", encoding="utf-8") as f:
        schema_sql = f.read()

    seed_sql = ""
    if os.path.exists(seed_file):
        with open(seed_file, "r", encoding="utf-8") as f:
            seed_sql = f.read()
    else:
        print(f"[WARN] Seed file not found: {seed_file}")

    try:
        conn = sqlite3.connect(db_file)
        conn.execute("PRAGMA foreign_keys = ON;")
        cursor = conn.cursor()

        # Execute schema
        print("[1/3] Executing schema DDL...")
        cursor.executescript(schema_sql)
        print("      Schema tables and indexes created successfully.")

        # Check if table already has data before seeding
        cursor.execute("SELECT count(*) FROM cars;")
        existing_cars = cursor.fetchone()[0]

        if existing_cars == 0 and seed_sql.strip():
            print("[2/3] Seeding initial data...")
            cursor.executescript(seed_sql)
            conn.commit()
            print("      Seed data loaded successfully.")
        else:
            print(f"[2/3] Skipping seed data (cars table already contains {existing_cars} rows).")

        # Verify table records
        print("[3/3] Verifying database integrity...")
        cursor.execute("SELECT count(*) FROM cars;")
        cars_count = cursor.fetchone()[0]

        cursor.execute("SELECT count(*) FROM customers;")
        cust_count = cursor.fetchone()[0]

        cursor.execute("SELECT count(*) FROM rentals;")
        rent_count = cursor.fetchone()[0]

        cursor.execute("SELECT category, count(*) FROM cars GROUP BY category;")
        cat_counts = cursor.fetchall()

        cursor.execute("SELECT status, count(*) FROM cars GROUP BY status;")
        status_counts = cursor.fetchall()

        conn.close()

        print("-" * 60)
        print("DATABASE READY FOR APPLICATION USE")
        print(f"  Total Cars     : {cars_count} {dict(cat_counts)}")
        print(f"  Fleet Status   : {dict(status_counts)}")
        print(f"  Total Customers: {cust_count}")
        print(f"  Total Rentals  : {rent_count}")
        print("=" * 60)
        return True

    except sqlite3.Error as e:
        print(f"[ERROR] SQLite execution failure: {e}", file=sys.stderr)
        return False

def main():
    parser = argparse.ArgumentParser(description="Initialize Group 10 Car Rental System SQLite Database.")
    parser.add_argument("--db", dest="db_file", default=None, help="Path to target .db file")
    parser.add_argument("--schema", dest="schema_file", default=None, help="Path to schema.sql")
    parser.add_argument("--seed", dest="seed_file", default=None, help="Path to seed_data.sql")
    parser.add_argument("--reset", action="store_true", help="Delete existing database before creating")
    args = parser.parse_args()

    success = init_database(args.db_file, args.schema_file, args.seed_file, args.reset)
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()
