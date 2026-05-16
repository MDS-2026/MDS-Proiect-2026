package com.mdsproject.backend.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notifications")
@Getter
@Setter
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private FairPayGroup group;

    @Column(nullable = false, length = 500)
    private String message;

    private String targetUrl;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;
}
