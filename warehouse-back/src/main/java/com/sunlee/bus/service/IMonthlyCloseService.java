package com.sunlee.bus.service;

import com.sunlee.bus.entity.MonthlyCloseRecord;
import com.sunlee.bus.vo.MonthlyCloseStatement;

public interface IMonthlyCloseService {

    MonthlyCloseStatement loadStatement(String yearMonth);

    MonthlyCloseRecord confirm(String yearMonth);
}
