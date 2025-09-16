-- 定义最大值和位数
local SECOND_FIELD_BITS = 14
local MAX_LIMIT_PER_PERSON = 2^SECOND_FIELD_BITS - 1

-- 将两个字段组合成一个long
local function combineFields(firstField, secondField)
    local firstFieldValue = firstField and 1 or 0
    return (firstFieldValue * 2 ^ SECOND_FIELD_BITS) + secondField
end

-- Lua脚本开始
local key = KEYS[1] -- 优惠券库存 Key
local userLimitKey = KEYS[2] -- 用户领取限制 Key
local validEndTime = tonumber(ARGV[1]) -- 优惠券模板结束时间
local limitPerPerson = tonumber(ARGV[2]) -- 用户领取限制数量

-- 获取当前时间
local currentTime = tonumber(redis.call('TIME')[1] .. redis.call('TIME')[2]) / 1000000

-- 检查优惠券是否已过期
if currentTime > validEndTime then
    return combineFields(false, 0)
end

-- 获取库存
local stock = tonumber(redis.call('HGET', key, 'stock'))

-- 检查库存是否大于0
if stock == nil or stock <= 0 then
    return combineFields(false, 0)
end

-- 获取用户已领取数量
local userReceivedCount = tonumber(redis.call('GET', userLimitKey)) or 0

-- 检查用户是否达到领取上限
if userReceivedCount >= limitPerPerson then
    return combineFields(false, userReceivedCount)
end

-- 自减库存
redis.call('HINCRBY', key, 'stock', -1)

-- 增加用户领取数量
redis.call('INCR', userLimitKey)

-- 设置用户领取限制 Key 的过期时间
local expireTime = validEndTime - currentTime
if expireTime > 0 then
    redis.call('EXPIRE', userLimitKey, math.floor(expireTime))
end

-- 获取用户当前领取数量
local currentUserReceivedCount = userReceivedCount + 1

-- 返回结果
return combineFields(true, currentUserReceivedCount)