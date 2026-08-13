package com.kawser.cleanspringbootproject.secretmode.application.port.in;

import com.kawser.cleanspringbootproject.secretmode.application.dto.UseHintCommand;

public interface UseHintUseCase {

    void useHint(UseHintCommand command);
}
