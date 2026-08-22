package com.kawser.cleanspringbootproject.user.application.port.in;

import com.kawser.cleanspringbootproject.user.application.dto.UpdateProfileCommand;
import com.kawser.cleanspringbootproject.user.application.dto.UpdateProfileResult;

public interface UpdateProfileUseCase {

    UpdateProfileResult updateProfile(UpdateProfileCommand command);
}
