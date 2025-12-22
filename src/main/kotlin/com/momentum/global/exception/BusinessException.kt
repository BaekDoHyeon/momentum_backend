package com.momentum.global.exception

import com.momentum.global.enums.ErrorCode

class BusinessException(
    val errorCode: ErrorCode,
) : RuntimeException(errorCode.message)