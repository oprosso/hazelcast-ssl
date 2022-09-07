package com.hazelcast.instance.impl;

public class SSLNodeContext extends DefaultNodeContext {
  @Override
  public NodeExtension createNodeExtension(Node node) {
    return new SSLNodeExtension(node);
  }
}
