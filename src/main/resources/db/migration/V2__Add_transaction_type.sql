ALTER TABLE transactions ADD COLUMN type VARCHAR(20);

UPDATE transactions SET type = 'TRANSFER' 
WHERE from_wallet_id IS NOT NULL AND to_wallet_id IS NOT NULL;

UPDATE transactions SET type = 'DEPOSIT' 
WHERE from_wallet_id IS NULL AND to_wallet_id IS NOT NULL;

UPDATE transactions SET type = 'WITHDRAWAL' 
WHERE from_wallet_id IS NOT NULL AND to_wallet_id IS NULL;

ALTER TABLE transactions ALTER COLUMN type SET NOT NULL;
