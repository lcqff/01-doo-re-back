package doore.member.application.dto.request;

import lombok.Builder;

@Builder
public record MemberUpdateRequest(
        String newName
) {
}
