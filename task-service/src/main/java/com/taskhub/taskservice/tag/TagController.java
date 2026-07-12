package com.taskhub.taskservice.tag;

import com.taskhub.taskservice.tag.dto.TagResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// Uses the fully-qualified io.swagger.v3.oas.annotations.tags.Tag below (not
// imported) since this package already has its own Tag entity of the same name.
@RestController
@RequestMapping("/api/v1/tags")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Tags", description = "Read-only access to tags")
class TagController {

    private final TagService tagService;

    TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    @Operation(summary = "List tags", description = "Returns a paginated list of all tags.")
    @ApiResponse(responseCode = "200", description = "Page of tags")
    Page<TagResponse> list(Pageable pageable) {
        return tagService.list(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a tag by id")
    @ApiResponse(responseCode = "200", description = "Tag found")
    @ApiResponse(responseCode = "404", description = "Tag not found")
    TagResponse get(@PathVariable UUID id) {
        return tagService.get(id);
    }
}
