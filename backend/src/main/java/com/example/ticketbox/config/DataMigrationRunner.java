package com.example.ticketbox.config;

import com.example.ticketbox.model.Category;
import com.example.ticketbox.repository.CategoryRepository;
import com.example.ticketbox.repository.EventRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Order(1) // Must run before DevDataSeeder (Order 10)
@RequiredArgsConstructor
@Slf4j
public class DataMigrationRunner implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    @PersistenceContext
    private final EntityManager entityManager;

    // Mapping from old EventCategory enum name → new Category data
    private static final Map<String, String[]> CATEGORY_SEED = new LinkedHashMap<>();

    static {
        CATEGORY_SEED.put("MUSIC",      new String[]{"Âm nhạc",   "music",      "🎵", "1"});
        CATEGORY_SEED.put("SPORTS",     new String[]{"Thể thao",   "sports",     "⚽", "2"});
        CATEGORY_SEED.put("CONFERENCE", new String[]{"Hội nghị",   "conference", "🎤", "3"});
        CATEGORY_SEED.put("THEATER",    new String[]{"Sân khấu",   "theater",    "🎭", "4"});
        CATEGORY_SEED.put("FILM",       new String[]{"Điện ảnh",   "film",       "🎬", "5"});
        CATEGORY_SEED.put("WORKSHOP",   new String[]{"Workshop",   "workshop",   "🛠️", "6"});
        CATEGORY_SEED.put("OTHER",      new String[]{"Khác",       "other",      "📌", "7"});
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedCategories();
        migrateEventCategories();
    }

    private void seedCategories() {
        if (categoryRepository.count() > 0) {
            log.info("Categories already seeded, skipping.");
            return;
        }

        log.info("Seeding categories...");
        for (Map.Entry<String, String[]> entry : CATEGORY_SEED.entrySet()) {
            String[] data = entry.getValue();
            Category category = Category.builder()
                    .name(data[0])
                    .slug(data[1])
                    .icon(data[2])
                    .displayOrder(Integer.parseInt(data[3]))
                    .build();
            categoryRepository.save(category);
        }
        log.info("Seeded {} categories.", CATEGORY_SEED.size());
    }

    private void migrateEventCategories() {
        // Use native query to read legacy CATEGORY column and populate CATEGORY_ID
        int updated = 0;
        for (Map.Entry<String, String[]> entry : CATEGORY_SEED.entrySet()) {
            String enumName = entry.getKey();
            String slug = entry.getValue()[1];

            categoryRepository.findBySlug(slug).ifPresent(category -> {
                int count = entityManager.createNativeQuery(
                        "UPDATE EVENTS SET CATEGORY_ID = :categoryId WHERE CATEGORY = :enumName AND CATEGORY_ID IS NULL"
                )
                .setParameter("categoryId", category.getId())
                .setParameter("enumName", enumName)
                .executeUpdate();

                if (count > 0) {
                    log.info("Migrated {} event(s) from CATEGORY={} to CATEGORY_ID={}", count, enumName, category.getId());
                }
            });
        }
    }
}
