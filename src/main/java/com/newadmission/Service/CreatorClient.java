package com.newadmission.Service;

import com.newadmission.DTO.CreatedByResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "client-superadmin",
        url = "${client.superadmin.base-url}")
public interface CreatorClient {

    @GetMapping("/getNameByemail")
    CreatedByResponseDTO getCreatorByEmail(@RequestParam String email);
}
