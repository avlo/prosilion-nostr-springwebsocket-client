package com.prosilion.subdivisions.util;

import com.prosilion.subdivisions.client.reactive.NostrSingleRequestService;
import java.util.Scanner;

public final class EchoUtility {
  private final NostrSingleRequestService nostrSingleRequestService = new NostrSingleRequestService();

  public static void main(String[] args) {
    new EchoUtility().run();
  }

  private void run() {
    Scanner scanner = new Scanner(System.in);

    System.out.print("Enter first argument: ");
    String first = scanner.nextLine().replaceAll("\\s", "");

    System.out.print("Enter second argument: ");
    String second = scanner.nextLine();

    System.out.println(first);
    System.out.println(second);
  }
}
