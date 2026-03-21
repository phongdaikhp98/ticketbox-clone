package com.example.ticketbox.service;

import com.example.ticketbox.dto.CategoryRequest;
import com.example.ticketbox.dto.CategoryResponse;
import com.example.ticketbox.exception.BadRequestException;
import com.example.ticketbox.exception.ResourceNotFoundException;
import com.example.ticketbox.model.Category;
import com.example.ticketbox.repository.CategoryRepository;
import com.example.ticketbox.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category musicCategory;
    private Category sportsCategory;
    private CategoryRequest createRequest;

    @BeforeEach
    void setUp() {
        musicCategory = Category.builder()
                .id(1L)
                .name("Am nhac")
                .slug("am-nhac")
                .icon("music")
                .displayOrder(1)
                .build();
        sportsCategory = Category.builder()
                .id(2L)
                .name("The thao")
                .slug("the-thao")
                .icon("sports")
                .displayOrder(2)
                .build();
        createRequest = new CategoryRequest();
        createRequest.setName("Phim anh");
        createRequest.setSlug("phim-anh");
        createRequest.setIcon("movie");
        createRequest.setDisplayOrder(3);
    }

    @Test
    void getAllCategories_categoriesExist_returnsMappedListOrderedByDisplayOrder() {
        when(categoryRepository.findAllByOrderByDisplayOrderAsc())
                .thenReturn(List.of(musicCategory, sportsCategory));
        List<CategoryResponse> result = categoryService.getAllCategories();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Am nhac", result.get(0).getName());
        assertEquals("am-nhac", result.get(0).getSlug());
        assertEquals("music", result.get(0).getIcon());
        assertEquals(1, result.get(0).getDisplayOrder());
        assertEquals("The thao", result.get(1).getName());
    }

    @Test
    void getAllCategories_noCategoriesExist_returnsEmptyList() {
        when(categoryRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of());
        List<CategoryResponse> result = categoryService.getAllCategories();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void createCategory_validRequest_savesAndReturnsResponse() {
        Category savedCategory = Category.builder()
                .id(3L).name("Phim anh").slug("phim-anh").icon("movie").displayOrder(3).build();
        when(categoryRepository.existsByName("Phim anh")).thenReturn(false);
        when(categoryRepository.existsBySlug("phim-anh")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);
        CategoryResponse result = categoryService.createCategory(createRequest);
        assertNotNull(result);
        assertEquals(3L, result.getId());
        assertEquals("Phim anh", result.getName());
        assertEquals("phim-anh", result.getSlug());
        assertEquals("movie", result.getIcon());
        assertEquals(3, result.getDisplayOrder());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategory_duplicateName_throwsBadRequestException() {
        when(categoryRepository.existsByName("Phim anh")).thenReturn(true);
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> categoryService.createCategory(createRequest));
        assertTrue(ex.getMessage().contains("Phim anh"));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void createCategory_duplicateSlug_throwsBadRequestException() {
        when(categoryRepository.existsByName("Phim anh")).thenReturn(false);
        when(categoryRepository.existsBySlug("phim-anh")).thenReturn(true);
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> categoryService.createCategory(createRequest));
        assertTrue(ex.getMessage().contains("phim-anh"));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void createCategory_noSlugProvided_generatesSlugFromName() {
        CategoryRequest req = new CategoryRequest();
        req.setName("Rock Music");
        req.setDisplayOrder(5);
        Category saved = Category.builder().id(4L).name("Rock Music").slug("rock-music").displayOrder(5).build();
        when(categoryRepository.existsByName("Rock Music")).thenReturn(false);
        when(categoryRepository.existsBySlug("rock-music")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);
        CategoryResponse result = categoryService.createCategory(req);
        assertNotNull(result);
        assertEquals("rock-music", result.getSlug());
    }

    @Test
    void createCategory_nullDisplayOrder_defaultsToZero() {
        CategoryRequest req = new CategoryRequest();
        req.setName("New Cat");
        req.setSlug("new-cat");
        req.setDisplayOrder(null);
        Category saved = Category.builder().id(5L).name("New Cat").slug("new-cat").displayOrder(0).build();
        when(categoryRepository.existsByName("New Cat")).thenReturn(false);
        when(categoryRepository.existsBySlug("new-cat")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);
        CategoryResponse result = categoryService.createCategory(req);
        assertNotNull(result);
        assertEquals(0, result.getDisplayOrder());
    }

    @Test
    void updateCategory_found_updatesAndReturnsResponse() {
        CategoryRequest updateReq = new CategoryRequest();
        updateReq.setName("Am nhac Updated");
        updateReq.setSlug("am-nhac-updated");
        updateReq.setIcon("music-updated");
        updateReq.setDisplayOrder(10);
        Category updatedCat = Category.builder()
                .id(1L).name("Am nhac Updated").slug("am-nhac-updated")
                .icon("music-updated").displayOrder(10).build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(musicCategory));
        when(categoryRepository.existsByName("Am nhac Updated")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(updatedCat);
        CategoryResponse result = categoryService.updateCategory(1L, updateReq);
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Am nhac Updated", result.getName());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void updateCategory_notFound_throwsResourceNotFoundException() {
        CategoryRequest updateReq = new CategoryRequest();
        updateReq.setName("Does Not Exist");
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.updateCategory(99L, updateReq));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updateCategory_duplicateName_throwsBadRequestException() {
        CategoryRequest updateReq = new CategoryRequest();
        updateReq.setName("The thao");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(musicCategory));
        when(categoryRepository.existsByName("The thao")).thenReturn(true);
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> categoryService.updateCategory(1L, updateReq));
        assertTrue(ex.getMessage().contains("The thao"));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updateCategory_sameNameAsCurrentCategory_doesNotThrowDuplicateException() {
        CategoryRequest updateReq = new CategoryRequest();
        updateReq.setName("Am nhac");
        Category saved = Category.builder().id(1L).name("Am nhac").slug("am-nhac").icon("music").displayOrder(1).build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(musicCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);
        assertDoesNotThrow(() -> categoryService.updateCategory(1L, updateReq));
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void deleteCategory_found_noEvents_deletesSuccessfully() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(musicCategory));
        when(eventRepository.countByCategoryId(1L)).thenReturn(0L);
        assertDoesNotThrow(() -> categoryService.deleteCategory(1L));
        verify(categoryRepository).delete(musicCategory);
    }

    @Test
    void deleteCategory_notFound_throwsResourceNotFoundException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.deleteCategory(99L));
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void deleteCategory_hasEvents_throwsBadRequestException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(musicCategory));
        when(eventRepository.countByCategoryId(1L)).thenReturn(3L);
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> categoryService.deleteCategory(1L));
        assertTrue(ex.getMessage().contains("3"));
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void toCategoryResponse_category_mapsAllFieldsCorrectly() {
        CategoryResponse response = categoryService.toCategoryResponse(musicCategory);
        assertEquals(1L, response.getId());
        assertEquals("Am nhac", response.getName());
        assertEquals("am-nhac", response.getSlug());
        assertEquals("music", response.getIcon());
        assertEquals(1, response.getDisplayOrder());
    }
}
