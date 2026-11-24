-- 1. Top 10 accounts by transaction volume
SELECT a.account_number, COUNT(t.id) AS transaction_count
FROM account a
LEFT JOIN transaction t ON a.account_number = t.source_account OR a.account_number = t.destination_account
GROUP BY a.account_number ORDER BY transaction_count DESC LIMIT 10;

-- 2. Failed transactions grouped by reason
SELECT error_category, reason, COUNT(*) AS failures
FROM failed_transaction GROUP BY error_category, reason ORDER BY failures DESC;

-- 3. Daily transaction amount by type
SELECT DATE(created_at) AS transaction_date, type, SUM(amount) AS total_amount
FROM transaction GROUP BY DATE(created_at), type ORDER BY transaction_date, type;

-- 4. Accounts with more than N transactions (replace 10 as needed)
SELECT account_number, COUNT(*) AS transaction_count
FROM (
  SELECT source_account AS account_number FROM transaction WHERE source_account IS NOT NULL
  UNION ALL
  SELECT destination_account FROM transaction WHERE destination_account IS NOT NULL
) x GROUP BY account_number HAVING COUNT(*) > 10;

-- 5. Complete transaction history for a customer (replace customer id)
SELECT c.customer_id, c.name, a.account_number, t.transaction_id, t.type, t.amount,
       t.source_account, t.destination_account, t.status, t.reason, t.created_at
FROM customer c JOIN account a ON a.customer_id = c.customer_id
LEFT JOIN transaction t ON t.source_account = a.account_number OR t.destination_account = a.account_number
WHERE c.customer_id = 1 ORDER BY t.created_at;

-- EXPLAIN ANALYZE examples to capture in the final report:
-- EXPLAIN ANALYZE SELECT * FROM transaction WHERE transaction_id = 'TXN100234';
-- EXPLAIN ANALYZE SELECT * FROM transaction WHERE status = 'FAILED' ORDER BY created_at DESC;
-- EXPLAIN ANALYZE SELECT type, SUM(amount) FROM transaction GROUP BY type;
