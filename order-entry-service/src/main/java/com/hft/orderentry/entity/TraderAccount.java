package com.hft.orderentry.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(schema = "accounts", name = "traders")
public class TraderAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String accountId;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true)
    private String apiKey;

    private double buyingPower;
    private double marginLimit;
}
