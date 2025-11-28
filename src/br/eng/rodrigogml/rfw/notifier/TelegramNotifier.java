package br.eng.rodrigogml.rfw.notifier;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Classe utilitária para enviar mensagens e arquivos ao Telegram via bot API usando InputStream.
 */
public class TelegramNotifier {

  private static final String API_URL = "https://api.telegram.org/bot";

  /**
   * Envia uma mensagem de texto para um chat específico.
   *
   * @param token Token do bot do Telegram.
   * @param chatId ID do chat ou grupo.
   * @param text Texto da mensagem.
   * @throws IOException Em caso de erro na comunicação HTTP.
   */
  public static void sendMessage(String token, String chatId, String text) throws IOException {
    String urlStr = API_URL + token + "/sendMessage?chat_id=" + chatId + "&text=" + urlEncode(text);
    sendGetRequest(urlStr);
  }

  /**
   * Envia um arquivo (documento, imagem, áudio ou vídeo) para um chat.
   *
   * @param token Token do bot do Telegram.
   * @param chatId ID do chat ou grupo.
   * @param inputStream Conteúdo do arquivo.
   * @param fileName Nome do arquivo (ex: "documento.pdf").
   * @param endpoint Endpoint da API (ex: "sendDocument", "sendPhoto").
   * @param fileFieldName Nome do campo no form-data (ex: "document", "photo").
   * @throws IOException Em caso de erro na comunicação HTTP.
   */
  private static void sendFile(String token, String chatId, InputStream inputStream, String fileName, String endpoint, String fileFieldName) throws IOException {
    String boundary = "===" + System.currentTimeMillis() + "===";
    String urlStr = API_URL + token + "/" + endpoint;
    URL url = new URL(urlStr);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

    conn.setDoOutput(true);
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

    try (DataOutputStream request = new DataOutputStream(conn.getOutputStream())) {
      // Campo chat_id
      request.writeBytes("--" + boundary + "\r\n");
      request.writeBytes("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n");
      request.writeBytes(chatId + "\r\n");

      // Campo arquivo
      request.writeBytes("--" + boundary + "\r\n");
      request.writeBytes("Content-Disposition: form-data; name=\"" + fileFieldName + "\"; filename=\"" + fileName + "\"\r\n");
      request.writeBytes("Content-Type: application/octet-stream\r\n\r\n");

      byte[] buffer = new byte[4096];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        request.write(buffer, 0, bytesRead);
      }

      request.writeBytes("\r\n");
      request.writeBytes("--" + boundary + "--\r\n");
      request.flush();
    }

    int responseCode = conn.getResponseCode();
    if (responseCode != HttpURLConnection.HTTP_OK) {
      InputStream errorStream = conn.getErrorStream();
      if (errorStream != null) {
        String response = new BufferedReader(new InputStreamReader(errorStream, "UTF-8"))
            .lines()
            .reduce("", (acc, line) -> acc + line + "\n");
        throw new IOException("Erro na requisição: " + response);
      }
    }
    conn.disconnect();
  }

  public static void sendDocument(String token, String chatId, InputStream inputStream, String fileName) throws IOException {
    sendFile(token, chatId, inputStream, fileName, "sendDocument", "document");
  }

  public static void sendPhoto(String token, String chatId, InputStream inputStream, String fileName) throws IOException {
    sendFile(token, chatId, inputStream, fileName, "sendPhoto", "photo");
  }

  public static void sendAudio(String token, String chatId, InputStream inputStream, String fileName) throws IOException {
    sendFile(token, chatId, inputStream, fileName, "sendAudio", "audio");
  }

  public static void sendVideo(String token, String chatId, InputStream inputStream, String fileName) throws IOException {
    sendFile(token, chatId, inputStream, fileName, "sendVideo", "video");
  }

  private static void sendGetRequest(String urlStr) throws IOException {
    URL url = new URL(urlStr);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    conn.getResponseCode();
    conn.disconnect();
  }

  private static String urlEncode(String value) throws UnsupportedEncodingException {
    return java.net.URLEncoder.encode(value, "UTF-8");
  }
}
