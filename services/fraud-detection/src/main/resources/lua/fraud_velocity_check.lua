-- fraud_velocity_check.lua
-- All velocity checks for ONE transaction in ONE round trip, atomically.
-- Requires Redis 5+ (writes after TIME). Not Redis Cluster: the 5 keys sit in different slots.
--
-- KEYS[1] = user lock           (1 txn / user   / lock window)
-- KEYS[2] = card lock           (1 txn / card   / lock window)
-- KEYS[3] = device lock         (1 txn / device / lock window)
-- KEYS[4] = ip lock             (1 txn / ip     / lock window)
-- KEYS[5] = user sliding window (sorted set: member = txnId, score = epoch ms)
--
-- ARGV[1] = txnId          (lock owner and sorted-set member => idempotent on Kafka redelivery)
-- ARGV[2] = lock TTL in seconds
-- ARGV[3] = sliding window size in ms
-- ARGV[4] = max txns allowed in the sliding window
--
-- Returns 5 flags, 1 = violation, 0 = ok, in this order:
--   {user, card, device, ip, burst}

-- ---- input validation: fail loudly instead of dying on a nil later ----
if #KEYS ~= 5 or #ARGV ~= 4 then
    return redis.error_reply('fraud_velocity_check: expected 5 keys and 4 args')
end

local txn = ARGV[1]
local ttl = tonumber(ARGV[2])
local window = tonumber(ARGV[3])
local limit = tonumber(ARGV[4])

if txn == '' or ttl == nil or ttl <= 0 or window == nil or window <= 0 or limit == nil or limit <= 0 then
    return redis.error_reply('fraud_velocity_check: invalid arguments')
end

local result = {}

-- ---- 1) Locks (user, card, device, ip): SET NX ----
-- If the lock is already owned by THIS txn (Kafka redelivery), it is not a violation.
for i = 1, 4 do
    local acquired = redis.call('SET', KEYS[i], txn, 'NX', 'EX', ttl)
    if acquired or redis.call('GET', KEYS[i]) == txn then
        result[i] = 0
    else
        result[i] = 1
    end
end

-- ---- 2) Sliding window burst check ----
-- Redis server time, so app-server clock skew is irrelevant.
local t = redis.call('TIME')
local now = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)

-- drop entries that fell out of the window
redis.call('ZREMRANGEBYSCORE', KEYS[5], 0, now - window)

if redis.call('ZSCORE', KEYS[5], txn) then
    result[5] = 0 -- already counted (redelivery)
else
    local count = redis.call('ZCARD', KEYS[5])
    if count >= limit then
        result[5] = 1 -- over the limit; blocked txn is NOT added to the window
    else
        redis.call('ZADD', KEYS[5], now, txn)
        result[5] = 0
    end
end

-- keep the set from living forever when the user goes quiet
redis.call('PEXPIRE', KEYS[5], window)

return result