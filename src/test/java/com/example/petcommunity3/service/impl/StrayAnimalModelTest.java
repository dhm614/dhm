package com.example.petcommunity3.service.impl;

import com.example.petcommunity3.entity.StrayAdoptionApplication;
import com.example.petcommunity3.entity.StrayAnimal;
import com.example.petcommunity3.entity.StrayAnimalReport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StrayAnimalModelTest {

    @Test
    void entitiesExposeStableStatusConstants() {
        assertThat(StrayAnimalReport.STATUS_PENDING).isZero();
        assertThat(StrayAnimalReport.STATUS_APPROVED).isEqualTo(1);
        assertThat(StrayAnimal.RESCUE_WAITING).isZero();
        assertThat(StrayAnimal.RESCUE_ADOPTION_PENDING).isEqualTo(1);
        assertThat(StrayAnimal.RESCUE_ADOPTED).isEqualTo(2);
        assertThat(StrayAdoptionApplication.STATUS_CANCELLED).isEqualTo(3);
    }
}
