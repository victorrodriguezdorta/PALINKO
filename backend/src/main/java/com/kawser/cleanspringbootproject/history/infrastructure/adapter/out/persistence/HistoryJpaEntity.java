package com.kawser.cleanspringbootproject.history.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "history")
public class HistoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String username;

    @Column(name = "\"day\"", nullable = false)
    private LocalDate challengeDate;

    @Column(name = "word_chain", nullable = false, columnDefinition = "TEXT")
    private String wordChain;

    protected HistoryJpaEntity() {
    }

    public HistoryJpaEntity(Long userId, String username, LocalDate challengeDate, String wordChain) {
        this.userId = userId;
        this.username = username;
        this.challengeDate = challengeDate;
        this.wordChain = wordChain;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setWordChain(String wordChain) {
        this.wordChain = wordChain;
    }
}
