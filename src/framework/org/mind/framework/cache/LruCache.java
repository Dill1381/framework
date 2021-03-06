/*
 * java.lang.Runtime类中的freeMemory(),totalMemory(),maxMemory()这几个方法的一些问题，
 * 很多人感到很疑惑，为什么，在java程序刚刚启动起来的时候freeMemory()这个方法返回的只有一两兆字节，
 * 而随着java程序往前运行，创建了不少的对象，freeMemory()这个方法的返回有时候不但没有减少，
 * 反而会增加。这些人对freeMemory()这个方法的意义应该有一些误解，他们认为这个方法返回的是操作系统的剩余可用内存，
 * 其实根本就不是这样的。这三个方法反映的都是java这个进程的内存情况，跟操作系统的内存根本没有关系。
 * 下面结合totalMemory(),maxMemory()一起来解释。
 *
 * maxMemory()这个方法返回的是java虚拟机（这个进程）能构从操作系统那里挖到的最大的内存，
 * 以字节为单位，如果在运行java程序的时候，没有添加-Xmx参数，那么就是64兆，
 * 也就是说maxMemory()返回的大约是64*1024*1024字节，这是java虚拟机默认情况下能从操作系统
 * 那里挖到的最大的内存。如果添加了-Xmx参数，将以这个参数后面的值为准，例如:
 * java -cp ClassPath -Xmx512m ClassName，那么最大内存就是512*1024*0124字节。
 *
 * totalMemory()这个方法返回的是java虚拟机现在已经从操作系统那里挖过来的内存大小，
 * 也就是java虚拟机这个进程当时所占用的所有内存。如果在运行java的时候没有添加-Xms参数，
 * 那么，在java程序运行的过程的，内存总是慢慢的从操作系统那里挖的，基本上是用多少挖多少，
 * 直挖到maxMemory()为止，所以totalMemory()是慢慢增大的。如果用了-Xms参数，
 * 程序在启动的时候就会无条件的从操作系统中挖 -Xms后面定义的内存数，然后在这些内存用的差不多的时候，再去挖。
 *
 * freeMemory()是什么呢，如果在运行java的时候没有添加-Xms参数，那么，在java程序运行的过程的，
 * 内存总是慢慢的从操作系统那里挖的，基本上是用多少挖多少，但是java虚拟机100％的情况下是会稍微多挖一点的，
 * 这些挖过来而又没有用上的内存，实际上就是 freeMemory()，所以freeMemory()的值一般情况下都是很小的，
 * 但是如果你在运行java程序的时候使用了-Xms，这个时候因为程序在启动的时候就会无条件的从操作系统中挖-Xms
 * 后面定义的内存数，这个时候，挖过来的内存可能大部分没用上，所以这个时候freeMemory()可能会有些大。
 */

package org.mind.framework.cache;

import org.apache.commons.lang.StringUtils;
import org.mind.framework.util.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 默认基于LRU(Least Recently Used)的缓存实现
 *
 * @author dongping
 * @date Sep 17, 2011
 */
public class LruCache extends AbstractCache implements Cacheable {

    private static final long serialVersionUID = -3563668641502091167L;

    private static final Logger log = LoggerFactory.getLogger(LruCache.class);

    /*
     * 保持活跃的缓存条目最大数,默认大小1024
     */
    private int cacheSize = 1024;

    /*
     * jvm 当前可用的内存大小
     */
    private long freeMemory = 0;

    /*
     * 条目最大超时设置
     */
    private long timeout = 0;

    private Map<String, Object> itemsMap;

    private transient final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private transient final Lock read = readWriteLock.readLock();

    private transient final Lock write = readWriteLock.writeLock();

    public static Cacheable initCache() {
        return CacheHolder.cacheInstance;
    }

    private static class CacheHolder {
        private static Cacheable cacheInstance = new LruCache();
    }

    private LruCache() {
        itemsMap = new LinkedHashMap<String, Object>(cacheSize, 0.75F, true) {
            private static final long serialVersionUID = -6005019516032449081L;

            @Override
            protected boolean removeEldestEntry(Entry<String, Object> eldest) {
                boolean tooBig = this.size() > LruCache.this.cacheSize;
                if (tooBig) {
                    log.debug("Remove the last entry key: {}", eldest.getKey());
                }
                return tooBig;
            }
        };
    }

    /**
     * 添加一个新条目，如果该条目已经存在，将不做任何操作
     *
     * @param key
     * @param value
     * @author dongping
     */
    public Cacheable addCache(String key, Object value) {
        return addCache(key, value, false);
    }

    /**
     * 添加一个新条目
     *
     * @param key
     * @param value
     * @param check false:若条目存在，不做任何操作。 true:先移除存在的条目，再重新装入;
     * @author dongping
     */
    public Cacheable addCache(String key, Object value, boolean check) {
        // 这里的判断还有点问题>> DEFAULT_MAX_FREEMEMORY >
        // Runtime.getRuntime().freeMemory()
        write.lock();
        try {
            if (freeMemory > 0 && freeMemory > Runtime.getRuntime().freeMemory()) {
                if (log.isDebugEnabled())
                    log.debug("At present there is insufficient space, a clear java.util.Map of all objects");

                this.destroy();
                return this;

            } else if (!check && this.containsKey(key)) {
                if (log.isDebugEnabled())
                    log.debug("The Cache key already exists.");
                return this;

            } else if (check) {
                this.removeCache(key);
            }

            itemsMap.put(super.realKey(key), new CacheElement(value, DateFormatUtils.getTimeMillis(), 0));
            return this;
        } finally {
            write.unlock();
        }
    }

    @Override
    public CacheElement getCache(String key) {
        return this.getCache(key, timeout);
    }

    @Override
    public CacheElement getCache(String key, long interval) {
        read.lock();
        CacheElement element = null;
        try {
            element = (CacheElement) itemsMap.get(super.realKey(key));
            if (element == null)
                return null;
        } finally {
            read.unlock();
        }

        if (interval > 0 && (DateFormatUtils.getTimeMillis() - element.getFirstTime()) > interval) {
            this.removeCache(key);
            element = null;
            log.warn("Remove Cache key, The access time interval expires. key = {}", key);
            return element;
        }

        write.lock();
        try {
            element.recordVisited();// 记录访问次数
            element.recordTime(DateFormatUtils.getTimeMillis()); // 记录本次访问时间
            return element;
        } finally {
            write.unlock();
        }
    }

    @Override
    public void removeCache(String key) {
        write.lock();
        try {
            if (this.containsKey(key))
                itemsMap.remove(super.realKey(key));
        } finally {
            write.unlock();
        }
    }


    @Override
    public void removeCacheContains(String searchStr) {
        removeCacheContains(searchStr, null);
    }

    @Override
    public void removeCacheContains(String searchStr, String[] excludes) {
        removeCacheContains(searchStr, excludes, Cacheable.EQ_FULL);
    }

    @Override
    public void removeCacheContains(String searchStr, String[] excludes, int exclidesRule) {
        String[] keys = this.getKeys();
        if (keys == null || keys.length == 0)
            return;

        write.lock();
        try {
            for (String key : keys) {
                if (StringUtils.contains(key, searchStr)) {
                    if (excludes != null && excludes.length > 0) {// Exclude
                        boolean flag = false;
                        for (String exKey : excludes) {
                            flag = Cacheable.EQ_FULL == exclidesRule ? StringUtils.equals(key, exKey) : StringUtils.contains(key, exKey);
                            if (flag)
                                break;
                        }

                        if (flag)
                            continue;
                    }

                    itemsMap.remove(key);
                }
            }
        } finally {
            write.unlock();
        }
    }


    @Override
    protected void destroy() {
        super.destroy();
        if (!this.isEmpty()) {
            itemsMap.clear();
            itemsMap = null;
            if (log.isInfoEnabled())
                log.info("Destroy CacheManager, Clear all items");
        }
    }

    public boolean isEmpty() {
        return this.itemsMap == null || this.itemsMap.isEmpty();
    }

    @Override
    public CacheElement[] getCaches() {
        if (this.isEmpty())
            return null;

        return itemsMap.values().toArray(new CacheElement[]{});
    }

    @Override
    public String[] getKeys() {
        if (this.isEmpty())
            return null;

        return itemsMap.keySet().toArray(new String[]{});
    }

    @Override
    public boolean containsKey(String key) {
        read.lock();
        try {
            return this.itemsMap.containsKey(super.realKey(key));
        } finally {
            read.unlock();
        }
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public void setFreeMemory(long freeMemory) {
        this.freeMemory = freeMemory;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

}
