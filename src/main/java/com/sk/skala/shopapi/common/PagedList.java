package com.sk.skala.shopapi.common;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagedList<T> {
    private List<T> items;
    private int offset;
    private int count;
    private long totalElements;
}