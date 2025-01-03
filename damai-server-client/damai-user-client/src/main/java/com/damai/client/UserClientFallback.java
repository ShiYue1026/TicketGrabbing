package com.damai.client;

import com.damai.common.ApiResponse;
import com.damai.dto.TicketUserListDto;
import com.damai.enums.BaseCode;
import com.damai.vo.TicketUserVo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserClientFallback implements UserClient {

    @Override
    public ApiResponse<List<TicketUserVo>> list(TicketUserListDto ticketUserListDto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
}
