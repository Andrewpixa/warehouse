package com.sunlee.bus.service;

import com.sunlee.bus.entity.DailyCloseRecord;
import com.sunlee.bus.vo.DailyCloseChecklist;

import java.time.LocalDate;

public interface IDailyCloseService {

    DailyCloseChecklist loadChecklist(LocalDate bizDate);

    DailyCloseRecord confirm(LocalDate bizDate);
}
