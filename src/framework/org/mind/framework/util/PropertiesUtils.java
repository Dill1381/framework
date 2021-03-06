package org.mind.framework.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 读取*.properies文件。
 *
 * @author dongping
 */
public abstract class PropertiesUtils {

    private static final Logger logger = LoggerFactory.getLogger(PropertiesUtils.class);

    private static final String DEFAULT_PROPERTIES = "/frame.properties";

    /**
     * 默认参数为frame.properties
     *
     * @return Properties
     * @throws IOException
     * @author dongping
     */
    public static Properties getProperties() {
        return getProperties(PropertiesUtils.class.getResourceAsStream(DEFAULT_PROPERTIES));
    }

    /**
     * 需要指定properties属性文件的绝对路径
     *
     * @param resPath properties绝对文件路径
     * @return Properties
     * @throws IOException
     * @author dongping
     */
    public static Properties getProperties(String resPath) {
        try {
            return getProperties(new BufferedInputStream(new FileInputStream(resPath)));
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 需要指定properties输入流
     *
     * @param in 可以是BufferedInputStream或InputStream
     * @return Properties
     * @throws IOException
     * @author dongping
     */
    public static Properties getProperties(InputStream in) {
        Properties props = new Properties();
        try {
            try {
                props.load(in);
                return props;
            } finally {
                in.close();
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }


    public static long getLong(Properties property, String key) {
        String str = getString(property, key);
        if (str == null || str.trim().isEmpty())
            throw new IllegalArgumentException(String.format("Corresponding to the key value is empty or characters, For input string: \" %s \"", str));

        return Long.parseLong(str);
    }

    public static int getInteger(Properties property, String key) {
        String str = getString(property, key);
        if (str == null || str.trim().isEmpty())
            throw new IllegalArgumentException(String.format("Corresponding to the key value is empty or characters, For input string: \" %s \"", str));

        return Integer.parseInt(str);
    }

    public static String getString(Properties property, String key) {
        if (property == null) {
            logger.error("Get Properties object is null, Please call the method: getProperties()");
            return null;
        }

        String str = property.getProperty(key);
        logger.debug("{} = {}", key, str);
        return str;
    }

}
