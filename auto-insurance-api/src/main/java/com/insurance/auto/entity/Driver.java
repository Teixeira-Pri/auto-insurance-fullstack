package com.insurance.auto.entity;

import com.insurance.auto.enums.Gender;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "drivers")
@Data
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gender gender;

    @Column(name = "zip_code", nullable = false, length = 8)
    private String zipCode;

    @Column(name = "license_issue_date", nullable = false)
    private LocalDate licenseIssueDate;

    @Column(length = 100)
    private String email;

    @Column(length = 11)
    private String phone;

    @Column(name = "claims_history", nullable = false)
    private Integer claimsHistory = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
