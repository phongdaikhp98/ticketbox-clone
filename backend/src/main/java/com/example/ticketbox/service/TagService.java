package com.example.ticketbox.service;

import com.example.ticketbox.dto.TagResponse;
import com.example.ticketbox.model.Tag;
import com.example.ticketbox.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    public List<TagResponse> getAllTags() {
        return tagRepository.findAllByOrderByUsageCountDescNameAsc().stream()
                .map(this::toTagResponse)
                .toList();
    }

    public List<TagResponse> getPopularTags() {
        return tagRepository.findTop20ByOrderByUsageCountDescNameAsc().stream()
                .map(this::toTagResponse)
                .toList();
    }

    @Transactional
    public Set<Tag> resolveTagList(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Tag> result = new HashSet<>();
        for (String rawName : tagNames) {
            String normalized = rawName.trim().toLowerCase();
            if (normalized.isEmpty()) continue;

            Tag tag = tagRepository.findByNameIgnoreCase(normalized)
                    .orElseGet(() -> tagRepository.save(Tag.builder()
                            .name(normalized)
                            .slug(generateSlug(normalized))
                            .build()));
            result.add(tag);
        }
        return result;
    }

    @Transactional
    public void incrementUsageCounts(Set<Tag> tags) {
        for (Tag tag : tags) {
            tag.setUsageCount(tag.getUsageCount() + 1);
            tagRepository.save(tag);
        }
    }

    @Transactional
    public void decrementUsageCounts(Set<Tag> tags) {
        for (Tag tag : tags) {
            int newCount = Math.max(0, tag.getUsageCount() - 1);
            tag.setUsageCount(newCount);
            tagRepository.save(tag);
        }
    }

    public TagResponse toTagResponse(Tag tag) {
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .slug(tag.getSlug())
                .usageCount(tag.getUsageCount())
                .build();
    }

    private String generateSlug(String name) {
        return name.replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
