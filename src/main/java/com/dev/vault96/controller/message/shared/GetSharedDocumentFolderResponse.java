package com.dev.vault96.controller.message.shared;

import com.dev.vault96.controller.message.member.MemberInfo;
import com.dev.vault96.entity.shared.SharedDocumentFolder;
import com.dev.vault96.entity.user.Member;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetSharedDocumentFolderResponse {
    private SharedDocumentFolder sharedDocumentFolder;
    private MemberInfo memberInfo;
}
