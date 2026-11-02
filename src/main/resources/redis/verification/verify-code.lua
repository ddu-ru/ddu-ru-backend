-- Result: 0 verified, 1 missing/expired, 2 already verified,
-- 3 invalid code, 4 attempts exceeded, 5 invalid expiry.
local ttl = redis.call('PTTL', KEYS[1])
if ttl == -1 then return 5 end
if ttl <= 0 then return 1 end

local raw = redis.call('GET', KEYS[1])
if not raw then return 1 end
local ok, data = pcall(cjson.decode, raw)
if not ok or type(data) ~= 'table' then return 1 end
if data.status == 'VERIFIED' then return 2 end
if data.status ~= 'PENDING' or type(data.code) ~= 'string' or type(data.count) ~= 'number' then
    return 1
end

if data.code ~= ARGV[1] then
    data.count = data.count + 1
    if data.count > tonumber(ARGV[2]) then
        redis.call('DEL', KEYS[1])
        return 4
    end
    redis.call('SET', KEYS[1], cjson.encode(data), 'XX', 'KEEPTTL')
    return 3
end

data.status = 'VERIFIED'
redis.call('SET', KEYS[1], cjson.encode(data), 'XX', 'PX', ARGV[3])
return 0
