package com.sunlee.bus.service;

import com.sunlee.bus.vo.PharmaStatsResult;

public interface IPharmaStatsService {

    PharmaStatsResult purchaseStats(String startDate, String endDate);

    PharmaStatsResult salesStats(String startDate, String endDate);
}
