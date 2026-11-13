package com.itremotesupport.it_server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RemoteTaskResultRequest(

        boolean success,

        @NotBlank
        @Size(max = 10000)
        String resultMessage

) {
}
