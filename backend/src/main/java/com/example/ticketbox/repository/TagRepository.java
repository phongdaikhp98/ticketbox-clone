package com.example.ticketbox.repository;

import com.example.ticketbox.model.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByNameIgnoreCase(String name);

    Optional<Tag> findBySlug(String slug);

    List<Tag> findAllByOrderByUsageCountDescNameAsc();

    List<Tag> findTop20ByOrderByUsageCountDescNameAsc();
}
