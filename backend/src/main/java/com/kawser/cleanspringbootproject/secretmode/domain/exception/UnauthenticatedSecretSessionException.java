package com.kawser.cleanspringbootproject.secretmode.domain.exception;

public class UnauthenticatedSecretSessionException extends SecretGameDomainException {

    public UnauthenticatedSecretSessionException() {
        super("UNAUTHENTICATED_SESSION", "No authenticated session for this connection");
    }
}
