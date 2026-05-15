package com.team31.financetracker.contracts.feign;
import com.team31.financetracker.contracts.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@FeignClient(name = "user-service", url = "${feign.user-service.url}")
public interface UserServiceClient {
    @GetMapping("/api/users/{id}")
    UserDTO getUser(@PathVariable("id") Long id);

    @GetMapping("/api/users/{id}/profile")
    UserProfileDTO getUserProfile(@PathVariable("id") Long id);

    @GetMapping("/api/users/by-ids")
    List<UserDTO> getUsersByIds(@RequestParam("ids") List<Long> ids);
}
