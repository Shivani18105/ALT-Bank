package com.altbank.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "customer")
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customerId;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, unique = true, length = 160) private String email;
    @Column(nullable = false, length = 20) private String phone;
    @Column(nullable = false) private Instant createdAt = Instant.now();

    public Long getCustomerId() { return customerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Instant getCreatedAt() { return createdAt; }
}
