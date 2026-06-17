package com.genai.java.spring.ticket;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Long getId()                       { return id; }
    public UUID getCreatedBy()                { return createdBy; }
    public String getTitle()                  { return title; }
    public String getDescription()            { return description; }
    public TicketStatus getStatus()           { return status; }
    public LocalDateTime getCreatedAt()       { return createdAt; }

    public void setCreatedBy(UUID v)          { this.createdBy = v; }
    public void setTitle(String v)            { this.title = v; }
    public void setDescription(String v)      { this.description = v; }
    public void setStatus(TicketStatus v)     { this.status = v; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}