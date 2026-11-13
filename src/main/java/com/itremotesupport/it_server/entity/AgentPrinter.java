package com.itremotesupport.it_server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_printers")
@Getter
@Setter
public class AgentPrinter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Column(nullable = false)
    private String printerName;

    private String driverName;

    private String portName;

    private String manufacturer;

    private Integer majorVersion;

    private String infPath;

    private LocalDateTime collectedAt;
}