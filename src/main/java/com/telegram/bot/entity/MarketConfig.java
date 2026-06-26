package com.telegram.bot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@Entity
@Builder
@Table(name = "MARKET_CONFIG")
@NoArgsConstructor
@AllArgsConstructor
public class MarketConfig {

    @Id
    @Column(name = "CONFIG_METHOD")
    private String configMethod;

    @Column(name = "METHOD_DESCRIPTION")
    private String methodDescription;

    @Column(name = "FLAG")
    private Boolean flag;

    @Column(name = "UPDATED_TIME")
    private LocalDateTime updatedTime;


}
