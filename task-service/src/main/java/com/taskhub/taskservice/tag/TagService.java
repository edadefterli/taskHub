package com.taskhub.taskservice.tag;

import com.taskhub.taskservice.common.ResourceNotFoundException;
import com.taskhub.taskservice.tag.dto.TagResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + id));
    }
}
