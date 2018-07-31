package com.github.emailtohl.lib.jpa;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.domain.Pageable;

/**
 * 分页对象，可用于输出到前端
 * @author HeLei
 */
public class Paging<T> implements Serializable {
	private static final long serialVersionUID = -1388460014258670621L;
	private List<T> content;
	private long totalElements;
	private int pageSize;
	private int totalPages;
	private int pageNumber;
	private int offset;

	public Paging(List<T> content, Pageable pageable, long total) {
		this.content = content;
		this.totalElements = total;
		this.pageNumber = pageable.getPageNumber();
		this.pageSize = pageable.getPageSize();
		this.totalPages = (int) ((this.totalElements + this.pageSize - 1) / this.pageSize);
	}

	public List<T> getContent() {
		return content;
	}

	public void setContent(List<T> content) {
		this.content = content;
	}

	/**
	 * 总元素构造时完成，不提供写方法
	 * 
	 * @return 符合查询条件的总结果数
	 */
	public long getTotalElements() {
		return totalElements;
	}

	/**
	 * 页面尺寸构造时完成，不提供写方法
	 * 
	 * @return 每页大小
	 */
	public int getPageSize() {
		return pageSize;
	}

	/**
	 * 总页数在构造时根据总元素和页面尺寸计算获得，不提供写方法
	 * 
	 * @return 查询结果的总页数
	 */
	public int getTotalPages() {
		return totalPages;
	}

	/**
	 * 存储查询时的页码数，从第0页开始
	 * 
	 * @return 当前所在页
	 */
	public int getPageNumber() {
		return pageNumber;
	}

	public void setPageNumber(int pageNumber) {
		this.pageNumber = pageNumber;
		this.offset = pageNumber * this.pageSize;
	}

	/**
	 * 存储查询页码时，计算获得
	 * 
	 * @return 查询后第一个结果所在数据库的位移
	 */
	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
		this.pageNumber = offset / this.pageSize;
	}
}
