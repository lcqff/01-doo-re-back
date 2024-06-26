package doore.login.api;

import doore.login.application.LoginService;
import doore.login.application.dto.response.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Valid
@RequestMapping("/login")
public class LoginController {
    private final LoginService loginService;

    @GetMapping("/google")
    public ResponseEntity<LoginResponse> loginByGoogle(@RequestParam("code") final String code) {
        return ResponseEntity.ok(loginService.loginByGoogle(code));
    }
}
