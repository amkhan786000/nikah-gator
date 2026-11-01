package org.nikahgator.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.nikahgator.entity.AppUser;
import org.nikahgator.exception.ApiException;
import org.nikahgator.repository.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Every signed-in user edits their own details. The e-mail is the sign-in and cannot be changed here. */
@RestController
@RequestMapping("/api/auth/profile")
public class ProfileController {

    public record ProfileRequest(@NotBlank @Size(max = 120) String fullName, @Size(max = 30) String phone,
                                 @Size(max = 80) String jobTitle) {
    }

    private final AppUserRepository users;

    public ProfileController(AppUserRepository users) {
        this.users = users;
    }

    @GetMapping
    public Map<String, Object> get(@AuthenticationPrincipal AppUser user) {
        return view(user);
    }

    @PutMapping
    public Map<String, Object> update(@AuthenticationPrincipal AppUser user, @Valid @RequestBody ProfileRequest req) {
        AppUser u = users.findById(user.getId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        u.setFullName(req.fullName().trim());
        u.setPhone(blankToNull(req.phone()));
        u.setJobTitle(blankToNull(req.jobTitle()));
        users.save(u);
        return view(u);
    }

    private static Map<String, Object> view(AppUser u) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("fullName", u.getFullName());
        out.put("email", u.getEmail());
        out.put("phone", u.getPhone());
        out.put("jobTitle", u.getJobTitle());
        out.put("role", u.getRole());
        return out;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
