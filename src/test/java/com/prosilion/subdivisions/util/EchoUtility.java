package com.prosilion.subdivisions.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.codec.BaseMessageDecoder;
import com.prosilion.nostr.enums.Command;
import com.prosilion.nostr.event.EventIF;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.message.EventMessage;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.nostr.util.Util;
import com.prosilion.subdivisions.client.reactive.NostrSingleRequestService;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * compile standalone utility
 * ./gradlew echoUtilityJar --quiet
 * <p>
 * run:
 * java -jar build/libs/echo-utility.jar
 */
public final class EchoUtility {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final String wsPrefix = "ws://%s";
  private final String urlTemplate = String.format(wsPrefix, "localhost:%s");
  private final NostrSingleRequestService nostrSingleRequestService = new NostrSingleRequestService();

  public static void main(String[] args) throws JsonProcessingException {
    new EchoUtility().run();
  }

  private void run() throws JsonProcessingException {
    Scanner scanner = new Scanner(System.in);

    System.out.print("Enter JSON request argument: ");
    String inputJson = readJson(scanner);

    System.out.print("Enter port (or full URL for non ws://localhost:<port_number> format) argument: ");
    String inputUrl = scanner.nextLine();

    String composedUrl = inputUrl.contains("localhost") ?
       String.format(wsPrefix, inputUrl) : String.format(urlTemplate, inputUrl);

    BaseMessage reqMessage = BaseMessageDecoder.decode(inputJson);
    if (!Command.REQ.equals(reqMessage.getCommand())) {
      throw new NostrException("not a REQ");
    }

    BaseMessage decode = ReqMessage.decode(Util.generateRandomHex64String(), inputJson);

    System.out.println("inputs:");
    System.out.println("  ".concat(decode.encode()));
    System.out.println("  ".concat(composedUrl));
    System.out.println();

    System.out.printf("sending JSON:\n  %s\n\nto URL\n  %s%n%n", decode.encode(), composedUrl);

    List<BaseMessage> baseMessages = nostrSingleRequestService.send((ReqMessage) reqMessage, composedUrl);
    List<EventIF> eventIFs = getEventIFs(baseMessages);
    System.out.println(eventIFs.stream().map(EventIF::createPrettyPrintJson).collect(Collectors.joining(",\n")));
  }

  private String readJson(Scanner scanner) throws JsonProcessingException {
    StringBuilder inputJson = new StringBuilder();

    while (scanner.hasNextLine()) {
      if (!inputJson.isEmpty()) {
        inputJson.append('\n');
      }
      inputJson.append(scanner.nextLine());

      if (isCompleteJson(inputJson)) {
        OBJECT_MAPPER.readTree(inputJson.toString());
        return inputJson.toString();
      }
    }

    throw new NostrException(inputJson.isEmpty() ? "empty JSON request" : "incomplete JSON request");
  }

  private boolean isCompleteJson(CharSequence json) {
    int depth = 0;
    boolean escaped = false;
    boolean inString = false;
    boolean started = false;

    for (int i = 0; i < json.length(); i++) {
      char current = json.charAt(i);
      if (inString) {
        if (escaped) {
          escaped = false;
        } else if (current == '\\') {
          escaped = true;
        } else if (current == '"') {
          inString = false;
        }
        continue;
      }

      if (current == '"') {
        inString = true;
      } else if (current == '[' || current == '{') {
        started = true;
        depth++;
      } else if (current == ']' || current == '}') {
        depth--;
        if (depth <= 0) {
          return true;
        }
      } else if (!started && !Character.isWhitespace(current)) {
        return true;
      }
    }

    return false;
  }

  public static List<EventIF> getEventIFs(List<BaseMessage> returnedBaseMessages) {
    return returnedBaseMessages.stream()
       .filter(EventMessage.class::isInstance)
       .map(EventMessage.class::cast)
       .map(EventMessage::getEvent)
       .toList();
  }
}
