package com.courseplatform.search.repository;

import com.courseplatform.search.document.CourseDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface CourseSearchRepository extends ElasticsearchRepository< CourseDocument, String > {
}
