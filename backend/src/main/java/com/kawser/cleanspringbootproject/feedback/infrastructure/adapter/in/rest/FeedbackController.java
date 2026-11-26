package com.kawser.cleanspringbootproject.feedback.infrastructure.adapter.in.rest;

import com.kawser.cleanspringbootproject.feedback.application.dto.SubmitFeedbackCommand;
import com.kawser.cleanspringbootproject.feedback.application.port.in.DismissCreatorLetterUseCase;
import com.kawser.cleanspringbootproject.feedback.application.port.in.GetCreatorLetterStatusUseCase;
import com.kawser.cleanspringbootproject.feedback.application.port.in.SubmitCreatorLetterFeedbackUseCase;
import com.kawser.cleanspringbootproject.feedback.infrastructure.adapter.in.rest.dto.CreatorLetterStatusResponse;
import com.kawser.cleanspringbootproject.feedback.infrastructure.adapter.in.rest.dto.SubmitFeedbackRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authenticated-only: covered by SecurityConfig's anyRequest().authenticated().
 */
@RestController
@RequestMapping("/feedback/creator-letter")
public class FeedbackController {

    private final GetCreatorLetterStatusUseCase getCreatorLetterStatusUseCase;
    private final SubmitCreatorLetterFeedbackUseCase submitCreatorLetterFeedbackUseCase;
    private final DismissCreatorLetterUseCase dismissCreatorLetterUseCase;

    public FeedbackController(
            GetCreatorLetterStatusUseCase getCreatorLetterStatusUseCase,
            SubmitCreatorLetterFeedbackUseCase submitCreatorLetterFeedbackUseCase,
            DismissCreatorLetterUseCase dismissCreatorLetterUseCase) {
        this.getCreatorLetterStatusUseCase = getCreatorLetterStatusUseCase;
        this.submitCreatorLetterFeedbackUseCase = submitCreatorLetterFeedbackUseCase;
        this.dismissCreatorLetterUseCase = dismissCreatorLetterUseCase;
    }

    @GetMapping
    public CreatorLetterStatusResponse getStatus(Authentication authentication) {
        return new CreatorLetterStatusResponse(
                getCreatorLetterStatusUseCase.shouldShowCreatorLetter(authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<Void> submit(
            Authentication authentication, @RequestBody @Valid SubmitFeedbackRequest request) {
        submitCreatorLetterFeedbackUseCase.submitCreatorLetterFeedback(
                new SubmitFeedbackCommand(authentication.getName(), request.suggestion(), request.issues()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/dismiss")
    public ResponseEntity<Void> dismiss(Authentication authentication) {
        dismissCreatorLetterUseCase.dismissCreatorLetter(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
