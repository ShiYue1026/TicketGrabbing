package com.damai.service.tool;

import com.damai.vo.SeatVo;

import java.util.*;

public class SeatMatch {

    public static List<SeatVo> findAdjacentSeatVos(List<SeatVo> allSeats, int seatCount) {
        List<SeatVo> adjacentSeats = new ArrayList<>();
        List<SeatVo> tempSeats = new ArrayList<>();

        if(allSeats.size() < seatCount) {
            return adjacentSeats;
        }

        // 对座位按行号和列号进行排序
        allSeats.sort((s1, s2) -> {
            if(Objects.equals(s1.getRowCode(), s2.getRowCode())) {
                return s1.getColCode() - s2.getColCode();
            } else{
                return s1.getColCode() - s2.getColCode();
            }
        });

        // 1. 找一排中相邻的座位
        for(int i=0; i < allSeats.size() - seatCount + 1; i++) {
            boolean seatsFound = true;
            for(int j=0; j < seatCount - 1; j++) {
                SeatVo current = allSeats.get(i + j);
                SeatVo next = allSeats.get(i + j + 1);

                if(!(Objects.equals(current.getRowCode(), next.getRowCode()) &&
                next.getColCode() - current.getColCode() == 1)) {
                    seatsFound = false;
                    break;
                }
            }
            if(seatsFound) {
                for(int k = 0; k < seatCount; k++) {
                    adjacentSeats.add(allSeats.get(i + k));
                }
            }
        }

        allSeats.sort((s1, s2) -> {
            if (Objects.equals(s1.getColCode(), s2.getColCode())) {
                return s1.getRowCode() - s2.getRowCode();
            } else {
                return s1.getColCode() - s2.getColCode();
            }
        });

        // 2. 对座位按行号和列号进行S型排序后随机选取
        Map<Integer, List<SeatVo>> rowMap = new HashMap<>();
        for (SeatVo seat : allSeats) {
            rowMap.computeIfAbsent(seat.getRowCode(), k -> new ArrayList<>()).add(seat);
        }

        List<SeatVo> sShapeSortedSeats = new ArrayList<>();
        for (Map.Entry<Integer, List<SeatVo>> entry : rowMap.entrySet()) {
            List<SeatVo> rowSeats = entry.getValue();
            if (entry.getKey() % 2 == 0) { // 偶数行，列号降序
                rowSeats.sort((s1, s2) -> s2.getColCode() - s1.getColCode());
            }
            sShapeSortedSeats.addAll(rowSeats);
        }

        allSeats.clear();
        allSeats.addAll(sShapeSortedSeats);

        if(allSeats.size() >= seatCount) {
            return allSeats.subList(0, seatCount);
        }

        return adjacentSeats;
    }
}
