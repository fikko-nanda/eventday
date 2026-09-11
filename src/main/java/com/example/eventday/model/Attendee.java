package com.example.eventday.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "order_attendees")
@Data
public class Attendee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orderId;

    private String fullName;
    private String email;
    private String phoneNumber;
    private String identityNumber; // NIK / No. KTP
}