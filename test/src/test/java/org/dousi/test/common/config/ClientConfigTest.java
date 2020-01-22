package org.dousi.test.common.config;

import org.dousi.config.ClientConfig;
import org.dousi.exception.DousiIllegalAddressException;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ClientConfigTest {

  @Test
  public void testGetServerPort() {
    ClientConfig clientConfig = ClientConfig.builder()
        .address("list://127.0.0.1:8080")
        .timeout(1000)
        .build();
    Assert.assertEquals(8080, clientConfig.getServerPort());
  }

  @Test(expectedExceptions = DousiIllegalAddressException.class)
  public void testGetServerPortDrpcIllegalAddressException() {
    ClientConfig clientConfig = ClientConfig.builder()
        .address("list://127.0.0.1:8080:8081")
        .timeout(1000)
        .build();
    clientConfig.getServerPort();

  }

  @Test(expectedExceptions = DousiIllegalAddressException.class)
  public void testGetServerPortNumberFormatException() {
    ClientConfig clientConfig = ClientConfig.builder()
        .address("list://127.0.0.1:abcd")
        .timeout(1000)
        .build();
    clientConfig.getServerPort();
  }

  @Test
  public void testGetServerIp() {
    ClientConfig clientConfig = ClientConfig.builder()
        .address("list://127.0.0.1:8080")
        .timeout(1000)
        .build();
    Assert.assertEquals("127.0.0.1", clientConfig.getServerIp());
  }

  @Test(expectedExceptions = DousiIllegalAddressException.class)
  public void testGetServerIpDrpcIllegalAddressException() {
    ClientConfig clientConfig = ClientConfig.builder()
        .address("list://127.0.0.1:8080:8081")
        .timeout(1000)
        .build();
    clientConfig.getServerIp();
  }
}
