/*
 * Copyright 2017 Ribose Inc. <https://www.ribose.com>
 * Copyright 2016 M-Way Solutions GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ribose.jenkins.plugin.awscodecommittrigger.logging;

import com.ribose.jenkins.plugin.awscodecommittrigger.SQSTriggerQueue;
import com.ribose.jenkins.plugin.awscodecommittrigger.model.job.SQSJob;
import hudson.model.Job;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang3.ClassUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.logging.*;


public class Log {

    private static final Set<Log> logs = Collections.synchronizedSet(new HashSet<Log>());

    private Logger logger;
    private Class clazz;
    private boolean fullLog = false;

    private Log(Class clazz) {
        this.clazz = clazz;
        this.logger = Logger.getLogger(this.clazz.getName());
    }

    public static Log get(final Class clazz) {
        Log log = new Log(clazz);
        logs.add(log);
        return (Log) CollectionUtils.find(logs, new Predicate() {
            @Override
            public boolean evaluate(Object o) {
                return ((Log)o).clazz.equals(clazz);
            }
        });
    }

    public static Log get(Class clazz, PrintStream out, boolean fullLog) {
        Log log = get(clazz);
        log.fullLog = fullLog;

        StreamHandler handler = new StreamHandler(out, new SimpleFormatter());
        log.logger.addHandler(handler);

        return log;
    }

    public static void addHandler(Handler handler) {
        for (Log log : logs) {
            log.logger.addHandler(handler);
        }
    }

    public void warning(final String message, final Object... args) {
        write(Level.WARNING, message, args);
    }

    public void error(final String message, final Object... args) {
        write(Level.SEVERE, message, args);
    }

    public void debug(final String message, final Object... args) {
        write(Level.CONFIG, message, args);
    }

    public void info(final String message, final Object... args) {
        write(Level.INFO, message, args);
    }

    private void write(final Level level, final String message, Object... args) {
        List<Object> params = new ArrayList<>();

        String jobName = null;
        Throwable error = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof SQSTriggerQueue) {
                args[i] = ((SQSTriggerQueue) args[i]).getUrl();
            } else if (args[i] instanceof Job) {
                jobName = ((Job) args[i]).getName();
                continue;
            } else if (args[i] instanceof SQSJob) {
                jobName = ((SQSJob) args[i]).getJenkinsJob().getName();
                continue;
            } else if (args[i] instanceof Throwable) {
                error = (Throwable) args[i];
                continue;
            }

            params.add(args[i]);
        }

        StringBuilder source = new StringBuilder();

        if (fullLog) {
            final String id = String.format("%06X", Thread.currentThread().getId());
            source
                .append("[").append(ClassUtils.getAbbreviatedName(this.clazz, 1)).append("]")
                .append("[thread-").append(id).append("]");

            if (jobName != null) {
                source.append("[job-").append(jobName).append("]");
            }
        }

        args = params.toArray();
        String msg = String.format(message, args);
        if (level == Level.CONFIG) {
            msg = "DEBUG: " + msg;
        } else if (level == Level.SEVERE) {
            msg = "ERROR: " + msg;
        }

        if (error == null) {
            this.logger.logp(level, source.toString(), "", msg);
        } else {
            this.logger.logp(level, source.toString(), "", msg, error);
        }

        Handler[] handlers = logger.getHandlers();
        for (Handler handler : handlers) {
            if (handler instanceof StreamHandler) {
                handler.flush();
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Log log = (Log) o;

        return clazz != null ? clazz.equals(log.clazz) : log.clazz == null;
    }

    @Override
    public int hashCode() {
        return clazz != null ? clazz.hashCode() : 0;
    }
}
