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
@Table (
	name = "enrollments",
	uniqueConstraints = {
		@UniqueConstraint(columnNames = {
			"user_id",
			"course_id"
		})
	}
)
@EqualsAndHashCode ( callSuper = true )
public class Enrollment extends Auditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "course_id", nullable = false)
	private Course course;

	@OneToMany(
		mappedBy = "enrollment",
		cascade = CascadeType.ALL,
		orphanRemoval = true
	)
	@Builder.Default
	private List<SubtopicProgress> progress = new ArrayList<>();
}
