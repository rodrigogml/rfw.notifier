package br.eng.rodrigogml.rfw.notifier;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Classe utilitária para envio de mensagens pelo Slack usando a Web API.
 */
public class SlackNotifier {

  private static final String API_URL = "https://slack.com/api/";
  private final String botToken;
  private final String defaultChannel;

  public SlackNotifier(String botToken, String defaultChannel) {
    this.botToken = botToken;
    this.defaultChannel = defaultChannel;
  }

  public SlackNotifier(String botToken) {
    this(botToken, null);
  }

  public void sendMessageToChannel(String channelId, String text) throws IOException {
    sendChatMessage(channelId, text);
  }

  public void sendMessageToDefaultChannel(String text) throws IOException {
    if (defaultChannel == null) {
      throw new IllegalStateException("Canal padrão não configurado");
    }
    sendChatMessage(defaultChannel, text);
  }

  public void sendMessageToConversation(String conversationId, String text) throws IOException {
    sendChatMessage(conversationId, text);
  }

  public void sendMessageToUser(String userId, String text) throws IOException {
    String conversationId = openImConversation(userId);
    sendChatMessage(conversationId, text);
  }

  private String openImConversation(String userId) throws IOException {
    String payload = "{\"users\":\"" + escapeJson(userId) + "\"}";
    String response = sendPostRequest("conversations.open", payload);
    String idTag = "\"id\":\"";
    int start = response.indexOf(idTag);
    if (start < 0) {
      throw new IOException(
          "Não foi possível obter o canal de IM para o usuário: " + userId + ". Resposta: " + response);
    }
    int from = start + idTag.length();
    int end = response.indexOf('"', from);
    if (end < 0) {
      throw new IOException("Resposta inválida ao abrir conversa: " + response);
    }
    return response.substring(from, end);
  }

  private void sendChatMessage(String channelId, String text) throws IOException {
    String payload =
        "{\"channel\":\"" + escapeJson(channelId) + "\",\"text\":\"" + escapeJson(text) + "\"}";
    sendPostRequest("chat.postMessage", payload);
  }

  private String sendPostRequest(String endpoint, String jsonPayload) throws IOException {
    URL url = new URL(API_URL + endpoint);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Authorization", "Bearer " + botToken);
    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
    conn.setDoOutput(true);

    try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
      wr.write(jsonPayload.getBytes("UTF-8"));
      wr.flush();
    }

    int responseCode = conn.getResponseCode();
    InputStream responseStream =
        responseCode >= 200 && responseCode < 300 ? conn.getInputStream() : conn.getErrorStream();
    String responseBody = readResponse(responseStream);
    conn.disconnect();

    if (responseCode != HttpURLConnection.HTTP_OK || !responseBody.contains("\"ok\":true")) {
      throw new IOException("Erro na requisição Slack: HTTP " + responseCode + " - " + responseBody);
    }
    return responseBody;
  }

  private String readResponse(InputStream responseStream) throws IOException {
    if (responseStream == null) {
      return "";
    }
    try (BufferedReader in = new BufferedReader(new InputStreamReader(responseStream, "UTF-8"))) {
      StringBuilder response = new StringBuilder();
      String inputLine;
      while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
      }
      return response.toString();
    }
  }

  private String escapeJson(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r");
  }
}
