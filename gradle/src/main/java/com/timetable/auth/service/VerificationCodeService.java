package com.timetable.auth.service;


import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VerificationCodeService {
    private final SecureRandom random = new SecureRandom();

    // email -> (code, expireAt)
    private static class Entry {
        final String code; final long expireAt;
        Entry(String code, long expireAt){ this.code=code; this.expireAt=expireAt; }
    }
    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final Map<String, Long> cooldown = new ConcurrentHashMap<>();

    public String generate6Digit() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    public boolean inCooldown(String email) {
        Long until = cooldown.get(email);
        return until != null && until > Instant.now().toEpochMilli();
    }

    public void putCooldown(String email, long seconds){
        cooldown.put(email, Instant.now().toEpochMilli() + seconds*1000);
    }

    public void saveCode(String email, String code, long ttlSeconds){
        long expire = Instant.now().toEpochMilli() + ttlSeconds*1000;
        store.put(email, new Entry(code, expire));
    }

    public boolean verify(String email, String code){
        Entry e = store.get(email);
        long now = Instant.now().toEpochMilli();
        if (e == null || e.expireAt < now) { store.remove(email); return false; }
        boolean ok = e.code.equals(code);
        if (ok) store.remove(email);
        return ok;
    }
}