package com.taskhub.taskservice.tag;

import com.taskhub.taskservice.tag.dto.TagResponse;

final class TagMapper {

    private TagMapper() {
    }

    static TagResponse toResponse(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName(), tag.getCreatedAt());
    }
}
