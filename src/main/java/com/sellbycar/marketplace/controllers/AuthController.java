package com.sellbycar.marketplace.controllers;

import com.sellbycar.marketplace.services.AuthService;
import com.sellbycar.marketplace.services.UserService;
import com.sellbycar.marketplace.services.impls.UserDetailsImpl;
import com.sellbycar.marketplace.utilities.handlers.ResponseHandler;
import com.sellbycar.marketplace.utilities.jwt.JwtUtils;
import com.sellbycar.marketplace.utilities.payload.request.LoginRequest;
import com.sellbycar.marketplace.utilities.payload.request.SignupRequest;
import com.sellbycar.marketplace.utilities.payload.response.JwtResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.security.auth.message.AuthException;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
@Tag(name = "Authentication Registration Library", description = "Endpoints for authentication user")
@CrossOrigin(origins = "*")
@Slf4j
public class AuthController {

    private final UserService userService;

    private final AuthenticationManager authenticationManager;

    private final JwtUtils jwtUtils;

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login User")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<?> authenticateUser(
            @Valid @RequestBody LoginRequest loginRequest,
            @RequestParam(name = "rememberMe", defaultValue = "false") boolean rememberMe
    ) {
        try {
            log.warn("Received login request for user: {}", loginRequest.getEmail());

            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            final String jwtAccessToken = jwtUtils.generateJwtToken(authentication);
            String jwtRefreshToken = null;

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            if (rememberMe) {
                jwtRefreshToken = jwtUtils.generateRefreshToken(authentication);
                authService.saveJwtRefreshToken(userDetails.getUsername(), jwtRefreshToken);
            }

            log.warn("User logged in successfully: {}", loginRequest.getEmail());

            return ResponseHandler.generateResponse("Token", HttpStatus.OK, new JwtResponse(jwtAccessToken, jwtRefreshToken));
        } catch (Exception e) {
            log.warn("Login attempt failed for user: {}", loginRequest.getEmail());
            return ResponseHandler.generateError("Login failed", HttpStatus.UNAUTHORIZED);
        }
    }


    @PostMapping("/signup")
    @Operation(summary = "Register User")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok"),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "409", description = "CONFLICT")
    })
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signUpRequest) throws MessagingException {
        if (userService.createNewUser(signUpRequest)) {
            return ResponseHandler.generateResponse("User registered successfully!", HttpStatus.OK);
        }
        return ResponseHandler.generateError("Email is already in use!", HttpStatus.CONFLICT);
    }

    @PostMapping("/refresh/access-token")
    @Operation(summary = "Refresh jwt access token")
    @ApiResponses({@ApiResponse(responseCode = "200", content = {@Content(schema = @Schema(implementation = JwtResponse.class), mediaType = "application/json")}),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> getNewAccessToken(@RequestBody JwtResponse response) throws AuthException {
        final JwtResponse token = authService.getJwtAccessToken(response.getJwtRefreshToken());
        return ResponseHandler.generateResponse("Access Token", HttpStatus.OK, token);
    }

    @PostMapping("/refresh/refresh-token")
    @Operation(summary = "Refresh jwt refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = {@Content(schema = @Schema(implementation = JwtResponse.class), mediaType = "application/json")}),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> getNewRefreshToken(@RequestBody JwtResponse response) throws AuthException {
        final JwtResponse token = authService.getJwtRefreshToken(response.getJwtRefreshToken());
        return ResponseHandler.generateResponse("Refresh token", HttpStatus.OK, token);
    }

    @PostMapping("/activate/{activationCode}")
    @Operation(summary = "Activate user with activation code")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ok"),
            @ApiResponse(responseCode = "400", description = "Bad Request")
    })
    public ResponseEntity<?> activateUser(@PathVariable("activationCode") String uniqueCode) {
        userService.activateUser(uniqueCode);
        return ResponseHandler.generateResponse("Ok", HttpStatus.OK);
    }
}
