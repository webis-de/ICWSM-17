package de.aitools.aq.wikipedia;

import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class BotList {
  
  private static final String RESOURCE_NAME = "bots.txt";
  
  private static Set<String> INSTANCE = null;
  
  public static boolean contains(final String name) {
    return BotList.get().contains(name);
  }
  
  private static synchronized final Set<String> get() {
    if (BotList.INSTANCE == null) {
      BotList.INSTANCE = BotList.read();
    }
    return BotList.INSTANCE;
  } 

  private static final Set<String> read() {
    final Set<String> bots = new HashSet<>();
    try (final Scanner reader = new Scanner(new InputStreamReader(
        BotList.class.getResourceAsStream(BotList.RESOURCE_NAME)))) {
      while (reader.hasNextLine()) {
        bots.add(reader.nextLine());
      }
    }
    return bots;
  }
  
}
