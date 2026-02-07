package com.courseplatform.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table (
	name = "subtopic_progress",
	uniqueConstraints = {
		@UniqueConstraint(columnNames = {
			"enrollment_id",
			"subtopic_id"
		})
	}
)
public class SubtopicProgress {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "enrollment_id", nullable = false)
	private Enrollment enrollment;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "subtopic_id", nullable = false)
	private Subtopic subtopic;

	@Column(nullable = false)
	private boolean completed;

	@Column(name = "completed_at")
	private ZonedDateTime completedAt;

	public void markCompleted() {
		this.completed = true;
		this.completedAt = ZonedDateTime.now();
	}
}
