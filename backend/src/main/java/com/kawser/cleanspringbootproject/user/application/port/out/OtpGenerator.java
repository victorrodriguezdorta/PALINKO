package com.kawser.cleanspringbootproject.user.application.port.out;

import com.kawser.cleanspringbootproject.user.domain.model.OneTimePassword;

public interface OtpGenerator {

    OneTimePassword generate();
}
