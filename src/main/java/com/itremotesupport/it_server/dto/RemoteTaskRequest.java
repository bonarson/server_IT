package com.itremotesupport.it_server.dto;

import com.itremotesupport.it_server.enums.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RemoteTaskRequest(

        @NotBlank
        String agentId,

        @NotNull
        TaskType taskType,

        @NotBlank
        @Size(max = 2000)
        String parametersJson

) {}