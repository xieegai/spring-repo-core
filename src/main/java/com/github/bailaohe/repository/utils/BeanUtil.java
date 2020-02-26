package com.github.bailaohe.repository.utils;

import com.google.common.collect.Lists;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.util.*;


/**
 * Created by baihe on 2017/3/24.
 */
public class BeanUtil {
    public static <S, D> D map(S source, Class<D> destinationClass) {
        Object target = null;
        try {
            target = destinationClass.newInstance();
            BeanUtils.copyProperties(source, target);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return (D) target;
    }

    public static <S, D> List<D> mapList(Iterable<S> sourceList, Class<D> destinationClass) {
        List<D> resList = Lists.newArrayList();
        sourceList.forEach(x -> resList.add(map(x, destinationClass)));
        return resList;
    }

    public static void copyPropertiesExcludeNULL(Object src, Object target) {
        if (null != src && null != target) {
            BeanUtils.copyProperties(src, target, getNullPropertyNames(src));
        }
    }

    public static String[] getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<String>();
        for (java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) emptyNames.add(pd.getName());
        }
        String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }

}
