package com.example.ticketbox.service;

import com.example.ticketbox.dto.TagResponse;
import com.example.ticketbox.model.Tag;
import com.example.ticketbox.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagService tagService;

    private Tag rockTag;
    private Tag popTag;

    @BeforeEach
    void setUp() {
        rockTag = Tag.builder()
                .id(1L)
                .name("rock")
                .slug("rock")
                .usageCount(10)
                .build();
        popTag = Tag.builder()
                .id(2L)
                .name("pop")
                .slug("pop")
                .usageCount(5)
                .build();
    }

    @Test
    void getAllTags_tagsExist_returnsSortedByUsageCountDesc() {
        when(tagRepository.findAllByOrderByUsageCountDescNameAsc())
                .thenReturn(List.of(rockTag, popTag));
        List<TagResponse> result = tagService.getAllTags();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("rock", result.get(0).getName());
        assertEquals(10, result.get(0).getUsageCount());
        assertEquals("pop", result.get(1).getName());
        assertEquals(5, result.get(1).getUsageCount());
    }

    @Test
    void getAllTags_noTagsExist_returnsEmptyList() {
        when(tagRepository.findAllByOrderByUsageCountDescNameAsc()).thenReturn(List.of());
        List<TagResponse> result = tagService.getAllTags();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getPopularTags_returnsTop20TagsSortedByUsageCountDesc() {
        when(tagRepository.findTop20ByOrderByUsageCountDescNameAsc())
                .thenReturn(List.of(rockTag, popTag));
        List<TagResponse> result = tagService.getPopularTags();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("rock", result.get(0).getName());
    }

    @Test
    void getPopularTags_noTags_returnsEmptyList() {
        when(tagRepository.findTop20ByOrderByUsageCountDescNameAsc()).thenReturn(List.of());
        List<TagResponse> result = tagService.getPopularTags();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void resolveTagList_newTagNames_createsAndReturnsTagEntities() {
        when(tagRepository.findByNameIgnoreCase("jazz")).thenReturn(Optional.empty());
        Tag newTag = Tag.builder().id(3L).name("jazz").slug("jazz").usageCount(0).build();
        when(tagRepository.save(any(Tag.class))).thenReturn(newTag);
        Set<Tag> result = tagService.resolveTagList(List.of("jazz"));
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(tagRepository).save(any(Tag.class));
    }

    @Test
    void resolveTagList_existingTagNames_returnsExistingTagsWithoutDuplicates() {
        when(tagRepository.findByNameIgnoreCase("rock")).thenReturn(Optional.of(rockTag));
        Set<Tag> result = tagService.resolveTagList(List.of("rock", "ROCK"));
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void resolveTagList_emptyList_returnsEmptySet() {
        Set<Tag> result = tagService.resolveTagList(List.of());
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(tagRepository, never()).findByNameIgnoreCase(anyString());
    }

    @Test
    void resolveTagList_nullList_returnsEmptySet() {
        Set<Tag> result = tagService.resolveTagList(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void resolveTagList_normalizesCaseAndTrimsWhitespace() {
        when(tagRepository.findByNameIgnoreCase("electronic")).thenReturn(Optional.empty());
        Tag newTag = Tag.builder().id(4L).name("electronic").slug("electronic").usageCount(0).build();
        when(tagRepository.save(any(Tag.class))).thenReturn(newTag);
        Set<Tag> result = tagService.resolveTagList(List.of("  ELECTRONIC  "));
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void resolveTagList_blankEntries_areSkipped() {
        when(tagRepository.findByNameIgnoreCase("rock")).thenReturn(Optional.of(rockTag));
        Set<Tag> result = tagService.resolveTagList(List.of("rock", "   ", ""));
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void incrementUsageCounts_tagSet_incrementsEachTagAndSaves() {
        Set<Tag> tags = Set.of(rockTag, popTag);
        tagService.incrementUsageCounts(tags);
        verify(tagRepository, times(2)).save(any(Tag.class));
        assertEquals(11, rockTag.getUsageCount());
        assertEquals(6, popTag.getUsageCount());
    }

    @Test
    void incrementUsageCounts_emptySet_savesNothing() {
        tagService.incrementUsageCounts(Set.of());
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void decrementUsageCounts_tagSet_decrementsEachTagAndSaves() {
        Set<Tag> tags = Set.of(rockTag, popTag);
        tagService.decrementUsageCounts(tags);
        verify(tagRepository, times(2)).save(any(Tag.class));
        assertEquals(9, rockTag.getUsageCount());
        assertEquals(4, popTag.getUsageCount());
    }

    @Test
    void decrementUsageCounts_tagWithZeroUsageCount_doesNotGoBelowZero() {
        Tag zeroTag = Tag.builder().id(5L).name("zero").slug("zero").usageCount(0).build();
        Set<Tag> tags = Set.of(zeroTag);
        tagService.decrementUsageCounts(tags);
        verify(tagRepository).save(any(Tag.class));
        assertEquals(0, zeroTag.getUsageCount());
    }

    @Test
    void decrementUsageCounts_emptySet_savesNothing() {
        tagService.decrementUsageCounts(Set.of());
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void toTagResponse_tag_mapsAllFieldsCorrectly() {
        TagResponse response = tagService.toTagResponse(rockTag);
        assertEquals(1L, response.getId());
        assertEquals("rock", response.getName());
        assertEquals("rock", response.getSlug());
        assertEquals(10, response.getUsageCount());
    }
}
