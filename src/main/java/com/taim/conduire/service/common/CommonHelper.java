package com.taim.conduire.service.common;

import com.taim.conduire.domain.UserData;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

//TODO --> Design Smell: Unutilized Abstraction.
public class CommonHelper {

    //TODO --> Design Smell: Deficient Encapsulation (public accessibility)
	public static ApplicationContext ctx = null;

    //TODO --> Design Smell: Deficient Encapsulation (public accessibility)
	private static final HashMap<String, ThreadPoolExecutor> poolMap = new HashMap<String, ThreadPoolExecutor>();
	private static final HashMap<String, String> nonceMap = new HashMap<String, String>();
	private static final HashMap<String, Integer> distance = new HashMap<String, Integer>();
	private static final HashMap<String, List<String>> mapStates = new HashMap<String, List<String>>();
	
	public static final synchronized Integer getDistance(String distanceString) {
		return distance.get(distanceString);
	}
	
	public static final synchronized void setDistance(String distanceString, Integer dis) {
		distance.put(distanceString, dis);
	}
	
	public static ThreadPoolExecutor getThreadPoolExecutor(String name) {
    	return poolMap.get(name);
    }
    public static void setThreadPoolExecutor(String name, ThreadPoolExecutor threadPoolExecutor) {
    	poolMap.put(name, threadPoolExecutor);
    }

    public static synchronized String getNonce(String shop) {
    	return nonceMap.get(shop);
    }
    public static synchronized void setNonce(String shop, String nonce) {
    	nonceMap.put(shop, nonce);
    }
    
	private static final HashMap<String, Object> mapCache = new HashMap<String, Object>();
   
	public static UserData getUserAccount() {
        final Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal instanceof UserData ? (UserData) principal : null;
    }

    public static boolean hasClassField(Class<?> clazz, final String field) {
        if (field == null) return true;

        Class<?> tempClass = clazz;
        do {
            try {
                tempClass.getDeclaredField(field);
                return true;
            } catch (NoSuchFieldException e) {
                tempClass = tempClass.getSuperclass();
            }
        } while (tempClass != null);

        return false;
    }
    
    public static synchronized Object getCache(String cacheId) {
    	return mapCache.get(cacheId);
    }
    
    public static synchronized void setCache(String cacheId, Object object) {
    	mapCache.put(cacheId, object);
    }
    
    public static synchronized int getDiffYears(Date first, Date last) {
        Calendar a = getCalendar(first);
        Calendar b = getCalendar(last);
        int diff = b.get(Calendar.YEAR) - a.get(Calendar.YEAR);
        if (a.get(Calendar.MONTH) > b.get(Calendar.MONTH) || 
            (a.get(Calendar.MONTH) == b.get(Calendar.MONTH) && a.get(Calendar.DATE) > b.get(Calendar.DATE))) {
            diff--;
        }
        return diff;
    }

    public static synchronized Calendar getCalendar(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal;
    }

	public static final void setStates(String state, List<String> states) {
		mapStates.put(state, states);
	}
	
	public static final List<String> getStates(String state) {
		return mapStates.get(state);
	}
}
