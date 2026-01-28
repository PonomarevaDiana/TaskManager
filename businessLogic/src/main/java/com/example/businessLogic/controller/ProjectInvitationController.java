package com.example.businessLogic.controller;

import com.example.businessLogic.entity.ProjectInvitation;
import com.example.businessLogic.service.ProjectInvitationService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/project-invitations")
@AllArgsConstructor
public class ProjectInvitationController {

    private ProjectInvitationService projectInvitationService;

    @GetMapping("/pending")
    public ResponseEntity<List<ProjectInvitation>> getPendingInvitations(Authentication authentication) {
        String userId = authentication.getPrincipal().toString();
        List<ProjectInvitation> pendingInvitations = projectInvitationService.getPendingInvitations(userId);

        return ResponseEntity.ok(pendingInvitations);
    }

    @PostMapping("/{invitationId}/accept")
    public ResponseEntity<Void> acceptInvitation(@PathVariable Long invitationId, Authentication authentication) {
        String userId = authentication.getPrincipal().toString();
        projectInvitationService.acceptInvitation(invitationId, userId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{invitationId}/decline")
    public ResponseEntity<Void> deleteInvitation(@PathVariable Long invitationId, Authentication authentication) {
        String userId = authentication.getPrincipal().toString();
        projectInvitationService.declineInvitation(invitationId, userId);

        return ResponseEntity.ok().build();
    }
}
