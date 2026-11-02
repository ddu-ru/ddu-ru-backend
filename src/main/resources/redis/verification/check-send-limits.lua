-- Result: {0, 0} allowed; {1, epochMillis} daily limit;
-- {2, epochMillis} resend limit; {3, 0} malformed/non-expiring limit.
-- Check and increment together so rejection never consumes the daily allowance.
local dailyTtl = redis.call('PTTL', KEYS[1])
if dailyTtl == -1 then return {3, 0} end
if dailyTtl == 0 then redis.call('DEL', KEYS[1]) end
local rawCount = redis.call('GET', KEYS[1])
local count = tonumber(rawCount or '0')
if not count or count < 0 or count ~= math.floor(count) then return {3, 0} end
if count >= tonumber(ARGV[1]) then
    return {1, redis.call('PEXPIRETIME', KEYS[1])}
end

local resendTtl = redis.call('PTTL', KEYS[2])
if resendTtl == -1 then return {3, 0} end
if resendTtl > 0 then
    return {2, redis.call('PEXPIRETIME', KEYS[2])}
end
if resendTtl == 0 then redis.call('DEL', KEYS[2]) end

redis.call('INCR', KEYS[1])
if not rawCount then redis.call('EXPIRE', KEYS[1], ARGV[2]) end
return {0, 0}
