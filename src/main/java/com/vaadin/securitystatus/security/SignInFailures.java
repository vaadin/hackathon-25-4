package com.vaadin.securitystatus.security;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Failed sign ins with the login view, counted per user name: after
 * {@link #ALLOWED} within {@link #WINDOW} the name is refused until the window
 * has passed, D-259. Under CAS the single sign on server does this itself.
 */
@Component
public class SignInFailures implements AuthenticationFailureHandler {

    static final int ALLOWED = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();
    private final AuthenticationFailureHandler then = new SimpleUrlAuthenticationFailureHandler("/login?error");
    private final Clock clock;

    public SignInFailures() {
        this(Clock.systemUTC());
    }

    SignInFailures(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        String name = request.getParameter("username");
        if (name != null && !name.isBlank()) {
            failed(name);
        }
        then.onAuthenticationFailure(request, response, exception);
    }

    void failed(String name) {
        Deque<Instant> times = failures.computeIfAbsent(key(name), any -> new ArrayDeque<>());
        synchronized (times) {
            forgetOld(times);
            times.addLast(clock.instant());
        }
    }

    boolean locked(String name) {
        Deque<Instant> times = failures.get(key(name));
        if (times == null) {
            return false;
        }
        synchronized (times) {
            forgetOld(times);
            return times.size() >= ALLOWED;
        }
    }

    /** The accounts, refusing a name while it is locked, whatever password comes with it. */
    public UserDetailsService guard(UserDetailsService users) {
        return name -> {
            if (locked(name)) {
                throw new LockedException(name);
            }
            UserDetails user = users.loadUserByUsername(name);
            return user;
        };
    }

    private void forgetOld(Deque<Instant> times) {
        Instant since = clock.instant().minus(WINDOW);
        while (!times.isEmpty() && times.peekFirst().isBefore(since)) {
            times.removeFirst();
        }
    }

    private static String key(String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }
}
