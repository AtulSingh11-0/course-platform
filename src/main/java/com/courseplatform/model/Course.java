package com.courseplatform.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table (name = "courses")
@EqualsAndHashCode ( callSuper = true )
public class Course extends Auditable {

	@Id
	@Column(nullable = false, unique = true)
	private String id;

	@Column(nullable = false)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String description;

	@OneToMany(
		mappedBy = "course",
		cascade = CascadeType.ALL,
		orphanRemoval = true
	)
	@Builder.Default
	private List<Topic> topics = new ArrayList<>();
}
