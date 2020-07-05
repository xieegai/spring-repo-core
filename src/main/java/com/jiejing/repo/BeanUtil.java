/*
 * This file is part of repo-core, which is free library: you can redistribute
 * it and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.jiejing.repo;

import com.google.common.collect.Lists;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.util.*;


/**
 * Created by baihe on 2017/3/24.
 */
class BeanUtil {
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
