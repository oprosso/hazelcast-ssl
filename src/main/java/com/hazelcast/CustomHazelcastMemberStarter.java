package com.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.impl.HazelcastInstanceFactory;
import com.hazelcast.instance.impl.SSLNodeContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import static com.hazelcast.internal.nio.IOUtil.closeResource;

public class CustomHazelcastMemberStarter {

  private CustomHazelcastMemberStarter() {
  }

  public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
    System.setProperty("hazelcast.tracking.server", "true");
    Config config = Config.load();

    HazelcastInstance hz = HazelcastInstanceFactory.newHazelcastInstance(
            config,
            config.getInstanceName(),
            new SSLNodeContext()
    );
    printMemberPort(hz);
  }

  private static void printMemberPort(HazelcastInstance hz) throws FileNotFoundException, UnsupportedEncodingException {
    String printPort = System.getProperty("print.port");
    if (printPort != null) {
      PrintWriter printWriter = null;
      try {
        printWriter = new PrintWriter("ports" + File.separator + printPort, "UTF-8");
        printWriter.println(hz.getCluster().getLocalMember().getAddress().getPort());
      } finally {
        closeResource(printWriter);
      }
    }
  }
}
