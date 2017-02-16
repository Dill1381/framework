package org.mind.framework.cache;

import org.mind.framework.util.DateFormat;

public class CacheElement {

	// 缓存的对象
	private Object value;

	// 缓存时记录的时间，如有访问过，就记录最近一次访问时间
	private long time;

	// 命中率次数
	private int visited;

	public CacheElement() {

	}

	public CacheElement(Object data, long time, int visited) {
		this.value = data;
		this.time = time;
		this.visited = visited;
	}

	public CacheElement(Object data) {
		this.value = data;
		this.time = DateFormat.getTimeMillis();
		this.visited = 0;
	}

	public void recordVisited() {
		visited ++;
	}
	
	public int getVisited() {
		return visited;
	}

	public void setVisited(int visited) {
		this.visited = visited;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public long getTime() {
		return time;
	}

	public void recordTime(long time) {
		this.time = time;
	}

}
