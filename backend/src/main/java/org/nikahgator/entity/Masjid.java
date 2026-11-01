package org.nikahgator.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "masjids")
@Getter @Setter
public class Masjid {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String slug;
    private String name;

    @Column(name = "address_line")
    private String addressLine;

    private String city;
    private String state;
    private String zip;
    private String phone;
    private String email;
    private String timezone = "America/New_York";

    @Column(name = "nikah_fee_cents")
    private int nikahFeeCents;

    @Column(name = "suggested_donation_cents")
    private int suggestedDonationCents;

    private boolean active = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
