package com.courseplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "topics")
public class Topic {

	@Id
	@Column(nullable = false, unique = true)
	private String id;

	@Column(nullable = false)
	private String title;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "course_id", nullable = false)
	@JsonIgnore
	@ToString.Exclude
	private Course course;

	@OneToMany(
		mappedBy = "topic",
		cascade = CascadeType.ALL,
		orphanRemoval = true
	)
	@Builder.Default
	private List<Subtopic> subtopics = new ArrayList<>();
}
