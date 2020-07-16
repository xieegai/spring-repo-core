package com.jiejing.repo.api;

import com.jiejing.repo.utils.PageUtil;
import lombok.Data;
import org.springframework.data.domain.Pageable;

/**
 * @author baihe created on 2019-01-23
 *
 * Paged request base class
 */
@Data
public abstract class PageRequest {

  /**
   * Current page index
   */
  private Long current = 0L;

  /**
   * Page size
   */
  private Long size = 10L;

  /**
   * Whether the page starts from 1
   */
  private boolean oneBasedPage = false;

  /**
   * Convert to Pageable object
   * @return the parsed Pageable
   */
  public Pageable toPageable() {
    return PageUtil.toPageable(current.intValue(), size.intValue(), null, oneBasedPage);
  }
}
