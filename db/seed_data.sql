-- Group 10 Car Rental System - Seed Data SQL
-- Rich initial dataset: Sedans, SUVs, Hatchbacks, Customers (tiers: REGULAR, SILVER, GOLD), Rentals (COMPLETED/RETURNED, ACTIVE)
-- Location: db/seed_data.sql

PRAGMA foreign_keys = ON;

-- ----------------------------------------------------------------------------
-- 1. Insert Cars (9 vehicles across 3 categories)
-- ----------------------------------------------------------------------------
-- Sedans
INSERT INTO cars (
    registration_number, make, model, year, category, daily_rate, mileage,
    status, is_available, seating_capacity, fuel_type, transmission,
    boot_space_liters, comfort_category, long_term_discount_percent
) VALUES
('DL-01-AB-2001', 'Honda', 'City ZX', 2023, 'SEDAN', 2800.0, 15000.0, 'AVAILABLE', 1, 5, 'PETROL', 'AUTOMATIC', 506.0, 'PREMIUM', 10.0),
('MH-02-CD-2002', 'Hyundai', 'Verna SX(O)', 2024, 'SEDAN', 2900.0, 11200.0, 'AVAILABLE', 1, 5, 'PETROL', 'MANUAL', 528.0, 'PREMIUM', 12.0),
('KA-03-EF-3003', 'Toyota', 'Camry Hybrid', 2023, 'SEDAN', 5200.0, 22400.0, 'AVAILABLE', 1, 5, 'HYBRID', 'AUTOMATIC', 524.0, 'LUXURY', 15.0);

-- SUVs
INSERT INTO cars (
    registration_number, make, model, year, category, daily_rate, mileage,
    status, is_available, seating_capacity, fuel_type, transmission,
    is_awd, ground_clearance_mm, is_seven_seater, all_terrain_surcharge
) VALUES
('DL-01-GH-4004', 'Mahindra', 'XUV700 AX7L', 2023, 'SUV', 4500.0, 29800.0, 'AVAILABLE', 1, 7, 'DIESEL', 'AUTOMATIC', 1, 200, 1, 400.0),
('KA-03-JK-5005', 'Hyundai', 'Creta SX(O)', 2024, 'SUV', 3500.0, 14300.0, 'RENTED', 0/*avail*/, 5, 'DIESEL', 'AUTOMATIC', 0/*awd*/, 190, 0/*7seat*/, 250.0),
('MH-02-LM-6006', 'Tata', 'Safari Dark Edition', 2023, 'SUV', 4800.0, 31200.0, 'MAINTENANCE', 0/*avail*/, 7, 'DIESEL', 'MANUAL', 0/*awd*/, 205, 1, 350.0);

-- Hatchbacks
INSERT INTO cars (
    registration_number, make, model, year, category, daily_rate, mileage,
    status, is_available, seating_capacity, fuel_type, transmission,
    is_compact_city, fuel_economy_kmpl, economy_discount_rate
) VALUES
('DL-01-NP-7007', 'Maruti Suzuki', 'Swift ZXi+', 2024, 'HATCHBACK', 1800.0, 8900.0, 'AVAILABLE', 1, 5, 'PETROL', 'MANUAL', 1, 22.5, 5.0),
('KA-03-RS-8008', 'Hyundai', 'i20 Asta(O)', 2023, 'HATCHBACK', 2100.0, 16700.0, 'RENTED', 0/*avail*/, 5, 'PETROL', 'AUTOMATIC', 1, 20.0, 5.0),
('MH-02-TU-9009', 'Tata', 'Altroz XZ+', 2023, 'HATCHBACK', 1900.0, 21500.0, 'AVAILABLE', 1, 5, 'CNG', 'MANUAL', 1, 26.0, 7.5);

-- ----------------------------------------------------------------------------
-- 2. Insert Customers (5 customers across REGULAR, SILVER, GOLD tiers)
-- ----------------------------------------------------------------------------
INSERT INTO customers (name, phone, driving_license, email, address, member_tier) VALUES
('Aarav Sharma', '9811012345', 'DL-1420110012345', 'aarav.sharma@example.com', '12 Connaught Place, New Delhi', 'REGULAR'),
('Priya Sundaram', '9822023456', 'MH-0220150023456', 'priya.sundaram@example.com', '45 Bandra West, Mumbai', 'SILVER'),
('Vikramaditya Roy', '9833034567', 'KA-0320180034567', 'vikram.roy@example.com', '88 Indiranagar, Bengaluru', 'GOLD'),
('Ananya Patel', '9844045678', 'GJ-0120190045678', 'ananya.patel@example.com', '102 SG Highway, Ahmedabad', 'SILVER'),
('Rohan Malhotra', '9855056789', 'DL-0420200056789', 'rohan.m@example.com', '77 Greater Kailash I, New Delhi', 'REGULAR');

-- ----------------------------------------------------------------------------
-- 3. Insert Rentals (3 past completed rentals and 2 currently active rentals)
-- ----------------------------------------------------------------------------
-- Completed Rentals
INSERT INTO rentals (
    customer_id, car_id, rental_date, expected_return_date, actual_return_date,
    rental_days, daily_rate, base_rent, discount_amount, insurance_fee, security_deposit,
    start_mileage, end_mileage, late_fee, damage_fee, total_amount, status, notes
) VALUES
-- Rental 1: Priya Sundaram (SILVER: 10% discount) rented Honda City (car 1, daily rate 2800) for 3 days
(2, 1, '2026-11-01', '2026-11-04', '2026-11-04',
 3, 2800.0, 8400.0, 840.0, 750.0, 3000.0,
 14300.0, 15000.0, 0.0, 0.0, 8310.0, 'COMPLETED',
 'Returned on time in spotless condition. Full refund of security deposit.'),

-- Rental 2: Vikramaditya Roy (GOLD: 20% discount) rented Swift (car 7, daily rate 1800) for 2 days, minor scratch
(3, 7, '2026-11-05', '2026-11-07', '2026-11-07',
 2, 1800.0, 3600.0, 720.0, 500.0, 3000.0,
 8400.0, 8900.0, 0.0, 500.0, 3880.0, 'COMPLETED',
 'Minor scratch on rear left bumper. Customer agreed to Rs 500 touch-up fee deducted from final settlement.'),

-- Rental 3: Rohan Malhotra (REGULAR: 0% discount) rented Safari (car 6, daily rate 4800) for 5 days
(5, 6, '2026-11-08', '2026-11-13', '2026-11-13',
 5, 4800.0, 24000.0, 0.0, 1250.0, 5000.0,
 29900.0, 31200.0, 0.0, 0.0, 25250.0, 'COMPLETED',
 'Returned with 1300km driven, dispatched for routine 30,000km scheduled maintenance post inspection.');

-- Active Rentals
INSERT INTO rentals (
    customer_id, car_id, rental_date, expected_return_date, actual_return_date,
    rental_days, daily_rate, base_rent, discount_amount, insurance_fee, security_deposit,
    start_mileage, end_mileage, late_fee, damage_fee, total_amount, status, notes
) VALUES
-- Active Rental 1: Aarav Sharma renting Hyundai i20 (car 8, daily rate 2100) for 4 days
(1, 8, '2026-11-18', '2026-11-22', NULL,
 4, 2100.0, 8400.0, 0.0, 1000.0, 3000.0,
 16700.0, NULL, 0.0, 0.0, 9400.0, 'ACTIVE',
 'Active rental - 4 day booking for highway trip.'),

-- Active Rental 2: Ananya Patel (SILVER: 10% discount) renting Hyundai Creta (car 5, daily rate 3500) for 4 days
(4, 5, '2026-11-17', '2026-11-21', NULL,
 4, 3500.0, 14000.0, 1400.0, 1000.0, 4000.0,
 14300.0, NULL, 0.0, 0.0, 13600.0, 'ACTIVE',
 'Active rental - Family weekend tour to Mysore with comprehensive insurance.');
