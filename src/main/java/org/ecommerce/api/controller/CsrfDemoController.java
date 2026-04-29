package org.ecommerce.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Educational endpoint showing how CSRF token protection works in a
 * session-based (stateful) application — the pattern used by traditional
 * Spring MVC / Thymeleaf form apps.
 *
 * This controller lives under /csrf-demo/** which is handled by the
 * csrfDemoFilterChain bean (SecurityConfig @Order(1)). That chain enables
 * CSRF + sessions deliberately so the handshake can be observed.
 *
 * The main JWT API (/api/**) keeps CSRF disabled because Bearer tokens are
 * not sent automatically by browsers, so there is nothing to forge.
 *
 * Postman walkthrough — run steps in order within the same session:
 *   1. GET  /csrf-demo/token          -> note the token value and the XSRF-TOKEN cookie
 *   2. POST /csrf-demo/submit  (no X-CSRF-TOKEN header) -> 403 Forbidden
 *   3. POST /csrf-demo/submit  (add  X-CSRF-TOKEN: <value from step 1>) -> 200 OK
 */
@Tag(name = "CSRF Demo", description = "Educational CSRF token demonstration (stateful session — not part of the JWT API)")
@RestController
@RequestMapping("/csrf-demo")
public class CsrfDemoController {

    @Operation(
        summary = "Get current CSRF token",
        description = """
            Returns the CSRF token for the current session and also sets the
            `XSRF-TOKEN` cookie. Pass the `token` value in the `X-CSRF-TOKEN`
            request header on any subsequent POST to this demo.

            **Why this exists**: CSRF tokens are server-generated, session-scoped
            secrets. A legitimate client (your own page) can read the token;
            a cross-origin attacker's page cannot — which is exactly what CSRF
            protection exploits.
            """
    )
    @GetMapping("/token")
    public Map<String, String> getToken(HttpServletRequest request) {
        // Spring's CsrfFilter has already resolved and stored the token as a
        // request attribute under the CsrfToken interface type.
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return Map.of(
            "token",         token.getToken(),
            "headerName",    token.getHeaderName(),    // "X-CSRF-TOKEN"
            "parameterName", token.getParameterName()  // "_csrf"
        );
    }

    @Operation(
        summary = "Form submission requiring CSRF token",
        description = """
            Simulates a state-changing form POST that requires a valid CSRF token.

            Spring Security's `CsrfFilter` intercepts this request **before** the
            controller runs. It compares the `X-CSRF-TOKEN` header (or `_csrf`
            request parameter) against the session-stored token. On mismatch it
            returns **403 Forbidden** immediately — the controller is never invoked.

            **Try it**:
            - Without the header → 403
            - With `X-CSRF-TOKEN: <value from GET /csrf-demo/token>` → 200
            """
    )
    @PostMapping("/submit")
    public Map<String, String> submit(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "(empty)");
        return Map.of(
            "status",   "accepted",
            "received", message
        );
    }
}
