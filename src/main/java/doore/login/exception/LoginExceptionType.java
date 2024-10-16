package doore.login.exception;

import doore.base.BaseExceptionType;
import org.springframework.http.HttpStatus;

public enum LoginExceptionType implements BaseExceptionType {
    NOT_FOUND_GOOGLE_ACCESS_TOKEN_RESPONSE(HttpStatus.NOT_FOUND, "Google Access Token 요청에 대한 응답이 null입니다."),
    EXPIRED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 Access Token입니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Access Token입니다."),
    NOT_FOUND_AUTHORIZATION_TOKEN(HttpStatus.NOT_FOUND, "Authorization Token을 찾을 수 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String errorMessage;

    LoginExceptionType(final HttpStatus httpStatus, final String errorMessage) {
        this.httpStatus = httpStatus;
        this.errorMessage = errorMessage;
    }

    @Override
    public HttpStatus httpStatus() {
        return httpStatus;
    }

    @Override
    public String errorMessage() {
        return errorMessage;
    }
}
