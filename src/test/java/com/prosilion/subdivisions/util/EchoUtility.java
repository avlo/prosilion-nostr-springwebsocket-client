package com.prosilion.subdivisions.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosilion.nostr.NostrException;
import com.prosilion.nostr.codec.BaseMessageDecoder;
import com.prosilion.nostr.enums.Command;
import com.prosilion.nostr.message.BaseMessage;
import com.prosilion.nostr.message.ReqMessage;
import com.prosilion.nostr.util.Util;
import com.prosilion.subdivisions.client.reactive.NostrSingleRequestService;
import java.util.Scanner;
import org.assertj.core.util.Strings;

/**
 * compile standalone utility
 * ./gradlew echoUtilityJar --quiet
 * <p>
 * run:
 * java -jar build/libs/echo-utility.jar
 */
public final class EchoUtility {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final String wsPrefix = "ws://";
  private final String urlTemplate = Strings.concat(
     wsPrefix, "localhost:%s");
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

    String composedUrl = inputUrl.contains("localhost") ? wsPrefix.concat(inputUrl) : urlTemplate.concat(inputUrl);

    BaseMessage reqMessage = BaseMessageDecoder.decode(inputJson);
    if (!Command.REQ.equals(reqMessage.getCommand())) {
      throw new NostrException("not a REQ");
    }

    BaseMessage decode = ReqMessage.decode(Util.generateRandomHex64String(), inputJson);

    System.out.println(decode.encode());
    System.out.println(composedUrl);
//    List<BaseMessage> baseMessages = nostrSingleRequestService.send((ReqMessage) reqMessage, composedUrl);

//    System.out.println(inputJson);
//    System.out.println(inputUrl);
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
}
