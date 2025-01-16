package com.damai.client;

import com.damai.common.ApiResponse;
import com.damai.dto.TicketUserListDto;
import com.damai.dto.UserGetAndTicketUserListDto;
import com.damai.vo.TicketUserVo;
import com.damai.vo.UserGetAndTicketUserListVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

import static com.damai.constant.Constant.SPRING_INJECT_PREFIX_DISTINCTION_NAME;

@Component
@FeignClient(value = SPRING_INJECT_PREFIX_DISTINCTION_NAME+"-"+"user-service", fallback  = UserClientFallback.class)
public interface UserClient {

    @PostMapping(value = "/ticket/user/list")
    ApiResponse<List<TicketUserVo>> list(TicketUserListDto ticketUserListDto);

    @PostMapping(value = "/user/get/user/ticket/list")
    ApiResponse<UserGetAndTicketUserListVo> getUserAndTicketUserList(UserGetAndTicketUserListDto dto);
}
