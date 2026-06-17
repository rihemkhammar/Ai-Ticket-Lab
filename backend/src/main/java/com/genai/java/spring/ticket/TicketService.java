package com.genai.java.spring.ticket;

import com.genai.java.spring.user.User;
import com.genai.java.spring.user.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TicketService {

    private final TicketRepository repo;
    private final UserRepository userRepository;

    public TicketService(TicketRepository repo, UserRepository userRepository) {
        this.repo = repo;
        this.userRepository = userRepository;
    }

    public List<Ticket> findAll() {
        return repo.findAll();
    }

    public Ticket findById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    public Ticket create(CreateTicketRequest request, String creatorUsername) {
        // Adapter findByEmail si UserRepository utilise un autre nom de méthode
        User creator = userRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));

        Ticket ticket = new Ticket();
        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedBy(creator.getId());
        ticket.setCreatedAt(LocalDateTime.now());

        return repo.save(ticket);
    }

    public Ticket updateStatus(Long id, TicketStatus status) {
        Ticket ticket = findById(id);
        ticket.setStatus(status);
        return repo.save(ticket);
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new TicketNotFoundException(id);
        }
        repo.deleteById(id);
    }
}