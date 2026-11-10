package com.example.petcommunity3.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.petcommunity3.entity.Appointment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AppointmentMapper extends BaseMapper<Appointment> {
}