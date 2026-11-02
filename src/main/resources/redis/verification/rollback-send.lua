redis.call('DEL', KEYS[1])
-- A failed SMS must not recreate an expired counter without a TTL.
local ttl = redis.call('PTTL', KEYS[2])
local count = tonumber(redis.call('GET', KEYS[2]) or '0')
if ttl > 0 and count and count > 0 then
    redis.call('DECR', KEYS[2])
end
return 1
