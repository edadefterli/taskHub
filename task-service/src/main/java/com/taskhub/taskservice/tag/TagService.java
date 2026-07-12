package com.taskhub.taskservice.tag;

import com.taskhub.taskservice.tag.dto.TagResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
class TagService {

    private final TagRepository tagRepository;

    TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    Page<TagResponse> list(Pageable pageable) {
        return tagRepository.findAll(pageable).map(TagMapper::toResponse);
    }

    @Transactional(readOnly = true)
    TagResponse get(UUID id) {
        return tagRepository.findById(id)
                .map(TagMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found: " + id));
    }
}
