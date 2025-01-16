package com.damai.lua;

import com.damai.vo.SeatVo;
import lombok.Data;

import java.util.List;

@Data
public class ProgramCacheCreateOrderData {

    private Integer code;

    private List<SeatVo> purchaseSeatList;
}
