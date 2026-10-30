package com.yue.jastodon.federation.note;

import com.yue.jastodon.federation.FederationProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@RestController
class NoteController {

    private final FederationProperties federationProperties;
    private final NoteRepository noteRepository;

    NoteController(FederationProperties federationProperties, NoteRepository noteRepository) {
        this.federationProperties = federationProperties;
        this.noteRepository = noteRepository;
    }

    @GetMapping("/users/{username}/statuses/{id}")
    ResponseEntity<NoteDocument> note(@PathVariable String username, @PathVariable String id) {
        if (!username.equals(federationProperties.username())) {
            return ResponseEntity.notFound().build();
        }

        Optional<UUID> noteId = parseCanonical(id);
        if (noteId.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Optional<Note> note = noteRepository.findById(noteId.get());
        if (note.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/activity+json"))
                .body(NoteDocument.of(note.get(), federationProperties));
    }

    private static Optional<UUID> parseCanonical(String id) {
        try {
            UUID uuid = UUID.fromString(id);
            return uuid.toString().equals(id) ? Optional.of(uuid) : Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
