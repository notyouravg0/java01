-- Group 10 Car Rental System - Database Schema DDL
-- Supported Engine: SQLite 3.x
-- Location: db/schema.sql

PRAGMA foreign_keys = ON;

-- ----------------------------------------------------------------------------
-- Table: cars
-- Polymorphic Single Table Inheritance for Sedan, SUV, Hatchback
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cars (
    car_id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    registration_number         TEXT NOT NULL UNIQUE,
    make                        TEXT NOT NULL,
    model                       TEXT NOT NULL,
    year                        INTEGER NOT NULL CHECK (year >= 2000 AND year <= 2035),
    category                    TEXT NOT NULL CHECK (category IN ('SEDAN', 'SUV', 'HATCHBACK', 'CAR')),
    daily_rate                  REAL NOT NULL CHECK (daily_rate > 0),
    mileage                     REAL NOT NULL DEFAULT 0.0 CHECK (mileage >= 0),
    status                      TEXT NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'RENTED', 'MAINTENANCE')),
    is_available                INTEGER NOT NULL DEFAULT 1 CHECK (is_available IN (0, 1)),
    seating_capacity            INTEGER NOT NULL CHECK (seating_capacity > 0),
    fuel_type                   TEXT NOT NULL CHECK (fuel_type IN ('PETROL', 'DIESEL', 'ELECTRIC', 'HYBRID', 'CNG')),
    transmission                TEXT NOT NULL CHECK (transmission IN ('MANUAL', 'AUTOMATIC')),
    
    -- Subclass: Sedan-specific attributes
    boot_space_liters           REAL CHECK (boot_space_liters IS NULL OR boot_space_liters > 0),
    comfort_category            TEXT CHECK (comfort_category IS NULL OR comfort_category IN ('EXECUTIVE', 'PREMIUM', 'LUXURY')),
    long_term_discount_percent  REAL DEFAULT 0.0 CHECK (long_term_discount_percent >= 0 AND long_term_discount_percent <= 100),
    
    -- Subclass: SUV-specific attributes
    is_awd                      INTEGER CHECK (is_awd IS NULL OR is_awd IN (0, 1)),
    ground_clearance_mm         INTEGER CHECK (ground_clearance_mm IS NULL OR ground_clearance_mm > 0),
    is_seven_seater             INTEGER CHECK (is_seven_seater IS NULL OR is_seven_seater IN (0, 1)),
    all_terrain_surcharge       REAL DEFAULT 0.0 CHECK (all_terrain_surcharge >= 0),
    
    -- Subclass: Hatchback-specific attributes
    is_compact_city             INTEGER CHECK (is_compact_city IS NULL OR is_compact_city IN (0, 1)),
    fuel_economy_kmpl           REAL CHECK (fuel_economy_kmpl IS NULL OR fuel_economy_kmpl > 0),
    economy_discount_rate       REAL DEFAULT 0.0 CHECK (economy_discount_rate >= 0 AND economy_discount_rate <= 100),
    
    created_at                  TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at                  TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

CREATE INDEX IF NOT EXISTS idx_cars_status ON cars(status);
CREATE INDEX IF NOT EXISTS idx_cars_category ON cars(category);
CREATE INDEX IF NOT EXISTS idx_cars_registration ON cars(registration_number);

-- ----------------------------------------------------------------------------
-- Table: customers
-- Customer profile with loyalty tier classification
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customers (
    customer_id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    name                        TEXT NOT NULL CHECK (length(trim(name)) > 0),
    phone                       TEXT NOT NULL UNIQUE CHECK (length(trim(phone)) >= 10),
    driving_license             TEXT NOT NULL UNIQUE CHECK (length(trim(driving_license)) >= 5),
    email                       TEXT,
    address                     TEXT,
    member_tier                 TEXT NOT NULL DEFAULT 'REGULAR' CHECK (member_tier IN ('REGULAR', 'SILVER', 'GOLD')),
    created_at                  TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at                  TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

CREATE INDEX IF NOT EXISTS idx_customers_phone ON customers(phone);
CREATE INDEX IF NOT EXISTS idx_customers_license ON customers(driving_license);
CREATE INDEX IF NOT EXISTS idx_customers_tier ON customers(member_tier);

-- ----------------------------------------------------------------------------
-- Table: rentals
-- Rental transactions linking customers and vehicles with full billing audit
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rentals (
    rental_id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id                 INTEGER NOT NULL,
    car_id                      INTEGER NOT NULL,
    rental_date                 TEXT NOT NULL,
    expected_return_date        TEXT NOT NULL,
    actual_return_date          TEXT,
    rental_days                 INTEGER NOT NULL DEFAULT 1 CHECK (rental_days >= 1),
    daily_rate                  REAL NOT NULL CHECK (daily_rate > 0),
    base_rent                   REAL NOT NULL DEFAULT 0.0 CHECK (base_rent >= 0),
    discount_amount             REAL NOT NULL DEFAULT 0.0 CHECK (discount_amount >= 0),
    insurance_fee               REAL NOT NULL DEFAULT 0.0 CHECK (insurance_fee >= 0),
    security_deposit            REAL NOT NULL DEFAULT 0.0 CHECK (security_deposit >= 0),
    start_mileage               REAL NOT NULL DEFAULT 0.0 CHECK (start_mileage >= 0),
    end_mileage                 REAL CHECK (end_mileage IS NULL OR end_mileage >= start_mileage),
    late_fee                    REAL NOT NULL DEFAULT 0.0 CHECK (late_fee >= 0),
    damage_fee                  REAL NOT NULL DEFAULT 0.0 CHECK (damage_fee >= 0),
    total_amount                REAL NOT NULL DEFAULT 0.0 CHECK (total_amount >= 0),
    status                      TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'COMPLETED', 'RETURNED', 'CANCELLED')),
    notes                       TEXT,
    created_at                  TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at                  TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (car_id) REFERENCES cars(car_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    
    CHECK (expected_return_date >= rental_date),
    CHECK (actual_return_date IS NULL OR actual_return_date >= rental_date)
);

CREATE INDEX IF NOT EXISTS idx_rentals_customer ON rentals(customer_id);
CREATE INDEX IF NOT EXISTS idx_rentals_car ON rentals(car_id);
CREATE INDEX IF NOT EXISTS idx_rentals_status ON rentals(status);
CREATE INDEX IF NOT EXISTS idx_rentals_dates ON rentals(rental_date, expected_return_date);
