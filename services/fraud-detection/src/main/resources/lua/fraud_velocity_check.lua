-- fraud_velocity_check.lua
-- Atomic velocity and burst check for account-to-account banking transactions.
-- Redis Cluster Safe: All keys share the {senderAccountNo} hash tag.
--
-- KEYS[1] = sender lock          (1 txn / sender account / lock window)
-- KEYS[2] = sender-receiver lock (1 txn / sender-receiver pair / lock window)
-- KEYS[3] = sender sliding window (sorted set: member = txnId, score = epoch ms)
--
-- ARGV[1] = txnId          (lock owner and sorted-set member => idempotent on Kafka redelivery)
-- ARGV[2] = lock TTL in seconds
-- ARGV[3] = sliding window size in ms
-- ARGV[4] = max txns allowed in sliding window
--
-- Returns 3 flags: 1 = violation / locked, 0 = ok
--   {sender_velocity, receiver_velocity, burst}

if #KEYS ~= 3 or #ARGV ~= 4 then
	return redis.error_reply('fraud_velocity_check: expected 3 keys and 4 args')
end

local txn = tostring(ARGV[1])
local ttl = tonumber(ARGV[2])
local window = tonumber(ARGV[3])
local limit = tonumber(ARGV[4])

if txn == '' or ttl == nil or ttl <= 0 or window == nil or window <= 0 or limit == nil or limit <= 0 then
	return redis.error_reply('fraud_velocity_check: invalid input arguments')
end

local result = {}

-- 1) Velocity Locks (sender account lock, sender-receiver pair lock) via atomic SET NX EX
-- If lock is already held by THIS transaction (redelivery/retry), it is permitted.
for i = 1, 2 do
	local acquired = redis.call('SET', KEYS[i], txn, 'NX', 'EX', ttl)
	if acquired or redis.call('GET', KEYS[i]) == txn then
		result[i] = 0
	else
		result[i] = 1
	end
end

-- 2) Sliding Window Burst Check on sender account using server TIME
local t = redis.call('TIME')
local now = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)

-- Expire transactions older than sliding window
redis.call('ZREMRANGEBYSCORE', KEYS[3], 0, now - window)

if redis.call('ZSCORE', KEYS[3], txn) then
	result[3] = 0 -- already counted (Kafka retry idempotency)
else
	local count = redis.call('ZCARD', KEYS[3])
	if count >= limit then
		result[3] = 1 -- over limit; violation flagged
	else
		redis.call('ZADD', KEYS[3], now, txn)
		result[3] = 0
	end
end

-- Refresh key TTL to window duration
redis.call('PEXPIRE', KEYS[3], window)

return result