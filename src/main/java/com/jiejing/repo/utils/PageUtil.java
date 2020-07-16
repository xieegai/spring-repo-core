package com.jiejing.repo.utils;


import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * @author baihe
 * Created on 2020-07-16 23:33
 */
public class PageUtil {
  /**
   * Convert to Pageable
   * @param pageNo the page index
   * @param pageSize the page size
   * @param sort the sort object
   * @param oneBasedPage whether the page start from 1
   * @return the converted Pageable object
   */
  public static final Pageable toPageable(int pageNo, int pageSize, Sort sort, boolean oneBasedPage) {
    return PageRequest.of(Math.max(pageNo - (oneBasedPage ? 1 : 0), 0), pageSize, sort);
  }
}
