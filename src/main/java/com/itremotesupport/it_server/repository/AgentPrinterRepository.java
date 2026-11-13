package com.itremotesupport.it_server.repository;

import com.itremotesupport.it_server.entity.AgentPrinter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentPrinterRepository
        extends JpaRepository<AgentPrinter, Long> {

    List<AgentPrinter> findByAgentAgentId(String agentId);

    void deleteByAgentAgentId(String agentId);
}