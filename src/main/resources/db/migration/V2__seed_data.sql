INSERT INTO customer(name,email,phone) VALUES
('Aarav Sharma','aarav@example.com','9000000001'),
('Meera Patil','meera@example.com','9000000002');
INSERT INTO account(customer_id,account_number,balance,status,currency)
SELECT customer_id,'ALT1000000001',10000.00,'ACTIVE','INR' FROM customer WHERE email='aarav@example.com';
INSERT INTO account(customer_id,account_number,balance,status,currency)
SELECT customer_id,'ALT1000000002',5000.00,'ACTIVE','INR' FROM customer WHERE email='meera@example.com';
