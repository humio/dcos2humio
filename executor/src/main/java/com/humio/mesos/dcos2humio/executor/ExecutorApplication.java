package com.humio.mesos.dcos2humio.executor;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import org.apache.mesos.MesosExecutorDriver;
import org.apache.mesos.Protos;

import java.io.File;

public class ExecutorApplication {
    public static void main(String[] args) {
        System.out.println("Starting executor");

        if (new File("config").mkdir()) {
            System.out.println("Created config directory");
        }

        MustacheFactory mustacheFactory = new DefaultMustacheFactory();
        final Mustache mustache = mustacheFactory.compile("tasksconfig.yaml.mustache");
        try {
            MesosExecutorDriver driver = new MesosExecutorDriver(new HumioExecutor(mustache));
            final Protos.Status status = driver.run();

            System.out.println("status = " + status);
            if (status.equals(Protos.Status.DRIVER_STOPPED)) {
                System.exit(0);
            } else {
                System.err.println("Error: " + status);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
