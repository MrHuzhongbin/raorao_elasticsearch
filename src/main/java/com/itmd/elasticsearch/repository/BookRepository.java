package com.itmd.elasticsearch.repository;

import com.itmd.elasticsearch.pojo.Books;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface BookRepository extends ElasticsearchRepository<Books,Long> {
}
