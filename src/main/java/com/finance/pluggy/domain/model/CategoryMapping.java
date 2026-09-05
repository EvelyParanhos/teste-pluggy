package com.finance.pluggy.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_category_mapping", indexes = {
    @Index(name = "idx_mapping_pluggy_cat", columnList = "pluggy_category", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class CategoryMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pluggy_category", nullable = false, unique = true)
    private String pluggyCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "internal_category", nullable = false)
    private InternalCategory internalCategory;
}
