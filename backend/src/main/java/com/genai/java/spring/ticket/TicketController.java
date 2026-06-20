package com.genai.java.spring.ticket;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    // Technicien + Admin
    @GetMapping
    public List<Ticket> list() {
        return service.findAll();
    }

    // Technicien + Admin
    @GetMapping("/{id}")
    public ResponseEntity<Ticket> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    // Technicien + Admin — créé par l'utilisateur connecté
    @PostMapping
    public ResponseEntity<Ticket> create(
            @Valid @RequestBody CreateTicketRequest request,
            Authentication authentication) {
        System.out.println(">>> auth.getName() = " + authentication.getName());
        System.out.println(">>> request.title  = " + request.getTitle());
        Ticket created = service.create(request, authentication.getName());
        return ResponseEntity.ok(created);
    }

    // Technicien + Admin
    @PatchMapping("/{id}")
    public ResponseEntity<Ticket> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateTicketStatusRequest request) {
        return ResponseEntity.ok(service.updateStatus(id, request.getStatus()));
    }

    // Admin uniquement
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}