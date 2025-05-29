package com.wizlit.file.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.wizlit.file.entity.View;

public interface ViewRepository extends ReactiveCrudRepository<View, String> {
}
