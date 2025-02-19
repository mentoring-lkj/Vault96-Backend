package com.dev.vault96.controller.message.tag;

import com.dev.vault96.entity.document.Tag;
import lombok.Getter;

import java.util.List;

@Getter
public class DeleteTagRequestBody {
    List<Tag> tags;
}
