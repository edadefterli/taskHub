package com.taskhub.taskservice.tag;

import com.taskhub.taskservice.common.SecurityConfig;
import com.taskhub.taskservice.tag.dto.TagResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TagController.class)
@Import(SecurityConfig.class)
class TagControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TagService tagService;

    @Test
    void should_returnPagedTags_when_listing() throws Exception {
        TagResponse response = new TagResponse(UUID.randomUUID(), "urgent", OffsetDateTime.now());
        Page<TagResponse> page = new PageImpl<>(List.of(response));

        when(tagService.list(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("urgent"));
    }

    @Test
    void should_returnTag_when_getById() throws Exception {
        UUID id = UUID.randomUUID();
        TagResponse response = new TagResponse(id, "urgent", OffsetDateTime.now());

        when(tagService.get(eq(id))).thenReturn(response);

        mockMvc.perform(get("/api/v1/tags/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("urgent"));
    }

    @Test
    void should_returnNotFound_when_tagDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();

        when(tagService.get(eq(id))).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found: " + id));

        mockMvc.perform(get("/api/v1/tags/{id}", id))
                .andExpect(status().isNotFound());
    }
}
