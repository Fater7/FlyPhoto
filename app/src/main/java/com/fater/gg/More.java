package com.fater.gg;

import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created on 2018-04-08.
 * 整个App的唯一多线程工具类
 */

public class More
{
    static int threadCount;      //线程池核心线程数
    static CompletionService<Integer> completionService = null;

    static
    {
        threadCount = Runtime.getRuntime().availableProcessors();
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(threadCount);
        completionService = new ExecutorCompletionService<>(fixedThreadPool);
    }
}
