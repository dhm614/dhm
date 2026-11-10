package com.example.petcommunity3.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.petcommunity3.entity.ServiceSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ServiceScheduleMapper extends BaseMapper<ServiceSchedule> {
    @Update("""
            UPDATE service_schedule
            SET booked_count = booked_count + 1
            WHERE id = #{id}
              AND status = 1
              AND booked_count < capacity
            """)
    int lockSlot(@Param("id") Long id);
}
