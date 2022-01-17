package com.springbootangular.backend.converter;

import org.springframework.core.convert.converter.Converter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Base Converter with default implementation for converting collections
 */
public interface BaseConverter<S, T> extends Converter<S, T> {
    default List<T> convertAll(List<S> sourceList) {
        return sourceList.stream().map(this::convert).collect(Collectors.toList());
    }
}