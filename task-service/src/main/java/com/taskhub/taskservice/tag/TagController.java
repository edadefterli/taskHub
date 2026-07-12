package com.taskhub.taskservice.tag;

import com.taskhub.taskservice.tag.dto.TagResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tags")
class TagController {

    private final TagService tagService;

    TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    Page<TagResponse> list(Pageable pageable) {
        return tagService.list(pageable);
    }

    @GetMapping("/{id}")
    TagResponse get(@PathVariable UUID id) {
        return tagService.get(id);
    }
}
