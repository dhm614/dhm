package com.example.petcommunity3.service.impl;

import com.example.petcommunity3.mapper.AppointmentMapper;
import com.example.petcommunity3.mapper.PetMapper;
import com.example.petcommunity3.mapper.PostCommentMapper;
import com.example.petcommunity3.mapper.PostMapper;
import com.example.petcommunity3.mapper.ProductMapper;
import com.example.petcommunity3.mapper.ProductOrderMapper;
import com.example.petcommunity3.mapper.TopicMapper;
import com.example.petcommunity3.mapper.UserMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppointmentStatusStatisticsTest {
    @Test
    void appointmentStatusIncludesPendingMerchantConfirmation() {
        AppointmentMapper appointmentMapper = mock(AppointmentMapper.class);
        when(appointmentMapper.selectCount(any())).thenReturn(0L);
        StatisticsServiceImpl service = new StatisticsServiceImpl(
                mock(UserMapper.class),
                mock(PetMapper.class),
                mock(PostMapper.class),
                mock(PostCommentMapper.class),
                appointmentMapper,
                mock(ProductMapper.class),
                mock(ProductOrderMapper.class),
                mock(TopicMapper.class)
        );

        assertThat(service.appointmentStatus())
                .anySatisfy(item -> {
                    assertThat(item.getStatus()).isEqualTo(3);
                    assertThat(item.getStatusName()).isEqualTo("待商户确认");
                });
    }
}
