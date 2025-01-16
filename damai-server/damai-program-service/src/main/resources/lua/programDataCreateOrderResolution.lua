-- 1：用户选择 2：自动匹配座位
local type = tonumber(KEYS[1])
-- 没有售卖的座位key(带%s)
local placeholder_seat_no_sold_hash_key = KEYS[2]
-- 锁定的座位key(带%s)
local placeholder_seat_lock_hash_key = KEYS[3];
-- 节目id
local program_id = KEYS[4]

-- 要购买的票档和数量、该票档的余票数量
local ticket_count_list = cjson.decode(ARGV[1])
-- 过滤后符合条件可以购买的座位集合
local purchase_seat_list = {}
-- 入参座位价格总和
local total_seat_dto_price = 0
-- 缓存座位价格总和
local total_seat_vo_price = 0

-- 自动匹配座位算法
local function find_adjacent_seats(all_seats, seat_count)
    local adjacent_seats = {}
    -- 按行号进行排序
    table.sort(all_seats, function(s1, s2)
        if s1.rowCode == s2.rowCode then
            return s1.colCode < s2.colCode
        else
            return s1.rowCode < s2.rowCode
        end
    end)

    for i = 1, #all_seats - seat_count + 1 do
        local seats_found = true
        for j = 0, seat_count - 2 do
            local curr = all_seats[i + j]
            local next = all_seats[i + j + 1]

            if not (curr.rowCode == next.rowCode and next.colCode - curr.colCode == 1) then
                seats_found = false
                break;
            end
        end
        if seats_found then
            for k=0, seat_count - 1 do
                table.insert(adjacent_seats, all_seats[i + k])
            end
            return adjacent_seats
        end
    end

    -- 按行号分组
    local rowMap = {}
    for _, seat in ipairs(all_seats) do
        if not rowMap[seat.rowCode] then
            rowMap[seat.rowCode] = {}
        end
        table.insert(rowMap[seat.rowCode], seat)
    end

    -- 按行号进行S型排序
    local sShapeSortedSeats = {}
    for rowCode, rowSeats in pairs(rowMap) do
        if rowCode % 2 == 0 then
            -- 偶数行按列号降序排序
            table.sort(rowSeats, function(s1, s2)
                return s1.colCode > s2.colCode
            end)
        else
            -- 奇数行按列号升序排序（默认排序）
            table.sort(rowSeats, function(s1, s2)
                return s1.colCode < s2.colCode
            end)
        end
        -- 添加排序后的座位到结果中
        for _, seat in ipairs(rowSeats) do
            table.insert(sShapeSortedSeats, seat)
        end
    end

    -- 如果座位数足够，返回前 seat_count 个座位
    if #sShapeSortedSeats >= seat_count then
        for i = 1, seat_count do
            table.insert(adjacent_seats, sShapeSortedSeats[i])
        end
    end

    return adjacent_seats
end


-- 开始校验
-- 如果是用户手动选座
if type == 1 then
     -- 验证余票数量
    for _, ticket_count in ipairs(ticket_count_list) do
        local ticket_remain_number_hash_key = ticket_count.programTicketRemainNumberHashKey
        local ticket_category_id = ticket_count.ticketCategoryId
        local count = ticket_count.ticketCount
        -- 从缓存中查询余票数量
        local remain_number_str = redis.call('hget', ticket_remain_number_hash_key, tostring(ticket_category_id))
        if not remain_number_str then
            return string.format('{"%s", %d}', 'code', 40010)
        end
        local remain_number = tonumber(remain_number_str)
        -- 用户选座数量大于库存中座位数量
        if(count > remain_number) then
            return string.format('{"%s": %d}', 'code', 40011)
        end
    end
    local seat_data_list = cjson.decode(ARGV[2])  -- 用户选择的座位集合
    for _, seatData in pairs(seat_data_list) do
        local seat_no_sold_hash_key = seatData.seatNoSoldHashKey
        local seat_dto_list = cjson.decode(seatData.seatDataList)
        for _, seat_dto in ipairs(seat_dto_list) do
            local id = seat_dto.id
            local seat_dto_price = seat_dto.price
            local seat_vo_str = redis.call('hget', seat_no_sold_hash_key, tostring(id))
            if not seat_vo_str then
                return string.format('{"%s": %d}', 'code', 40001)
            end
            local seat_vo = cjson.decode(seat_vo_str)
            -- 座位状态是锁定
            if(seat_vo.sellStatus == 2) then
                return string.format('{"%s": %d}', 'code', 40002)
            end
            -- 作为状态是已售卖
            if(seat_vo.sellStatus == 3) then
                return string.format('{"%s": %d}', 'code', 40003)
            end
            table.insert(purchase_seat_list, seat_vo)
            -- 入参座位总价格
            total_seat_dto_price = total_seat_dto_price + seat_dto_price
            -- 缓存座位总价格
            total_seat_vo_price = total_seat_vo_price + seat_vo.price
            if (total_seat_dto_price > total_seat_vo_price) then
                return string.format('{"%s": %d}', 'code', 40008)
            end
        end
    end
end

-- 自动匹配座位
if type == 2 then
    for _, ticket_count in ipairs(ticket_count_list) do
        local ticket_remain_number_hash_key = ticket_count.programTicketRemainNumberHashKey
        local ticket_category_id = ticket_count.ticketCategoryId
        local count = ticket_count.ticketCount
        -- 从缓存中获取相应票档数量
        local remain_number_str = redis.call('hget', ticket_remain_number_hash_key, tostring(ticket_category_id))
        if not remain_number_str then
            return string.format('{"%s": %d}', 'code', 40010)
        end
        local remain_number = tonumber(remain_number_str)
        if(count > remain_number) then
            return string.format('{"%s": %d}', 'code', 40011)
        end
        local seat_no_sold_hash_key = ticket_count.seatNoSoldHashKey
        local seat_vo_no_sold_str_list = redis.call('hvals',seat_no_sold_hash_key)
        local filter_seat_vo_no_sold_list = {}
        -- 这里遍历的原因，座位集合是以hash存储在缓存中，而每个座位是字符串，要把字符串转成对象
        for index,seat_vo_no_sold_str in ipairs(seat_vo_no_sold_str_list) do
            local seat_vo_no_sold = cjson.decode(seat_vo_no_sold_str)
            table.insert(filter_seat_vo_no_sold_list,seat_vo_no_sold)
        end
        -- 自动座位匹配算法
        purchase_seat_list = find_adjacent_seats(filter_seat_vo_no_sold_list, count)
        if #purchase_seat_list < count then
            return string.format('{"%s": %d}', 'code', 40004)
        end
    end
end

-- 验证完毕，开始锁定座位和扣除票档数量操作
local seat_id_list = {}
local seat_data_list = {}
for _,seat in ipairs(purchase_seat_list) do
    local seat_id = seat.id
    local ticket_category_id = seat.ticketCategoryId

    if not seat_id_list[ticket_category_id] then
        seat_id_list[ticket_category_id] = {}
    end
    table.insert(seat_id_list[ticket_category_id], tostring(seat_id))

    if not seat_data_list[ticket_category_id] then
        seat_data_list[ticket_category_id] = {}
    end
    -- 这里在放入值的时候先是放入了座位id
    table.insert(seat_data_list[ticket_category_id], tostring(seat_id))
    seat.sellStatus = 2
    -- 然后又放入了座位数据
    table.insert(seat_data_list[ticket_category_id], cjson.encode(seat))
end
-- 扣除票档数量
for _,ticket_count in ipairs(ticket_count_list) do
    local ticket_remain_number_hash_key = ticket_count.programTicketRemainNumberHashKey
    local ticket_category_id = ticket_count.ticketCategoryId
    local count = ticket_count.ticketCount
    redis.call('hincrby',ticket_remain_number_hash_key,ticket_category_id,"-" .. count)
end
-- 将选中的座位从未售卖中去除
for ticket_category_id, seat_id_array in pairs(seat_id_list) do
    redis.call('hdel',string.format(placeholder_seat_no_sold_hash_key, program_id, tostring(ticket_category_id)), unpack(seat_id_array))
end
-- 将选中的座位加入到锁定中
-- 再将座位数据添加到锁定的座位中
for ticket_category_id, seat_data_array in pairs(seat_data_list) do
    redis.call('hset',string.format(placeholder_seat_lock_hash_key, program_id, tostring(ticket_category_id)), unpack(seat_data_array))
end
return string.format('{"%s": %d, "%s": %s}', 'code', 0, 'purchaseSeatList', cjson.encode(purchase_seat_list))