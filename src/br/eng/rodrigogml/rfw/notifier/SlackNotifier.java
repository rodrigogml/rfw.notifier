package br.eng.rodrigogml.rfw.notifier;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import br.eng.rodrigogml.rfw.kernel.exceptions.RFWCriticalException;
import br.eng.rodrigogml.rfw.kernel.exceptions.RFWException;

/**
 * Classe utilitária para integração com a Slack Web API.
 *
 * Permite: - Envio de mensagens para canais, conversas e usuários (DM) - Abertura de conversas diretas (IM) - Consulta de usuários, canais e conversas - Execução de requisições genéricas para qualquer endpoint da API
 *
 * Requisitos: - Bot Token com escopos adequados: chat:write, conversations.open, conversations.read, users:read.
 *
 * @author Rodrigo
 * @since 10 de dez. de 2025
 */
public class SlackNotifier {

  private static final String API_URL = "https://slack.com/api/";
  private final String botToken;

  /**
   * Construtor principal.
   *
   * @param botToken Token do bot (xoxb-...) com permissões adequadas.
   */
  public SlackNotifier(String botToken) {
    this.botToken = botToken;
  }

  /**
   * Envia mensagem diretamente para um canal Slack.
   *
   * @param channelId ID do canal (Cxxxxxx)
   * @param text Texto da mensagem
   * @throws RFWException Erro ao enviar a mensagem
   */
  public void sendMessageToChannel(String channelId, String text) throws RFWException {
    sendChatMessage(channelId, text);
  }

  /**
   * Envia mensagem para uma conversa já existente ou thread.
   *
   * @param conversationId ID da conversa ou thread
   * @param text Mensagem
   * @throws RFWException Erro ao enviar a mensagem
   */
  public void sendMessageToConversation(String conversationId, String text) throws RFWException {
    sendChatMessage(conversationId, text);
  }

  /**
   * Envia mensagem diretamente para um usuário via DM. Internamente abre ou obtém o canal de IM para o usuário.
   *
   * @param userId ID do usuário (Uxxxxxx)
   * @param text Mensagem
   * @throws RFWException Erro ao enviar mensagem
   */
  public void sendMessageToUser(String userId, String text) throws RFWException {
    String conversationId = openImConversation(userId);
    sendChatMessage(conversationId, text);
  }

  /**
   * Abre (ou recupera) o canal de mensagem direta (IM) com o usuário informado. Endpoint utilizado: conversations.open
   *
   * @param userId ID do usuário Slack
   * @return ID do canal de DM (formato Dxxxxxx)
   * @throws RFWException Erro ao abrir a conversa
   */
  public String openImConversation(String userId) throws RFWException {
    String payload = "{\"users\":\"" + escapeJson(userId) + "\"}";
    String response = sendPostRequest("conversations.open", payload);

    String idTag = "\"id\":\"";
    int start = response.indexOf(idTag);
    if (start < 0) {
      throw new RFWCriticalException("Não foi possível obter o canal de IM para o usuário: " + userId + ". Resposta: " + response);
    }

    int from = start + idTag.length();
    int end = response.indexOf('"', from);
    if (end < 0) {
      throw new RFWCriticalException("Resposta inválida ao abrir conversa: " + response);
    }

    return response.substring(from, end);
  }

  /**
   * Envia mensagem usando o endpoint chat.postMessage.
   *
   * @param channelId ID do canal/conversa
   * @param text Texto da mensagem
   * @throws RFWException Em caso de falha
   */
  private void sendChatMessage(String channelId, String text) throws RFWException {
    String payload = "{\"channel\":\"" + escapeJson(channelId) + "\",\"text\":\"" + escapeJson(text) + "\"}";
    sendPostRequest("chat.postMessage", payload);
  }

  /**
   * Executa POST genérico para qualquer endpoint da Slack Web API.
   *
   * @param endpoint Nome do endpoint (ex: "users.list")
   * @param jsonPayload Corpo JSON enviado no POST
   * @return Corpo JSON da resposta
   * @throws RFWException Em caso de erro de transporte ou erro retornado pela API
   */
  private String sendPostRequest(String endpoint, String jsonPayload) throws RFWException {
    int responseCode;
    String responseBody;
    try {
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

      responseCode = conn.getResponseCode();
      InputStream responseStream = responseCode >= 200 && responseCode < 300 ? conn.getInputStream() : conn.getErrorStream();
      responseBody = readResponse(responseStream);
      conn.disconnect();

    } catch (Exception e) {
      throw new RFWCriticalException("Falha ao enviar POST para Slack!", e);
    }

    if (!responseBody.contains("\"ok\":true")) {
      throw new RFWCriticalException("Erro Slack: HTTP " + responseCode + " - " + responseBody);
    }

    return responseBody;
  }

  /**
   * Lê o conteúdo de um InputStream e devolve como String.
   *
   * @param responseStream Stream da resposta HTTP
   * @return Conteúdo da resposta
   * @throws RFWException Em caso de erro de leitura
   */
  private String readResponse(InputStream responseStream) throws RFWException {
    if (responseStream == null) return "";

    try (BufferedReader in = new BufferedReader(new InputStreamReader(responseStream, "UTF-8"))) {
      StringBuilder response = new StringBuilder();
      String inputLine;
      while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
      }
      return response.toString();

    } catch (Exception e) {
      throw new RFWCriticalException("Falha ao ler resposta do Slack!", e);
    }
  }

  /**
   * Escapa caracteres especiais para uso seguro dentro de JSON.
   *
   * @param value Texto original
   * @return Texto escapado
   */
  private String escapeJson(String value) {
    if (value == null) return "";
    return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
  }

  // ============================================================
  // CONSULTAS DA API — JSON PURO
  // ============================================================

  /**
   * Consulta a lista de usuários do workspace. Endpoint: users.list
   *
   * @return JSON retornado pelo Slack
   * @throws RFWException Erro de requisição
   */
  public String getUsers() throws RFWException {
    return sendPostRequest("users.list", "{}");
  }

  /**
   * Consulta informações detalhadas de um usuário. Endpoint: users.info
   *
   * @param userId ID do usuário (Uxxxx)
   * @return JSON da resposta
   * @throws RFWException Em caso de erro
   */
  public String getUserInfo(String userId) throws RFWException {
    String payload = "{\"user\":\"" + escapeJson(userId) + "\"}";
    return sendPostRequest("users.info", payload);
  }

  /**
   * Consulta os canais (públicos e privados). Endpoint: conversations.list
   *
   * @return JSON com os canais do workspace
   * @throws RFWException Erro na requisição
   */
  public String getChannels() throws RFWException {
    String payload = "{\"types\":\"public_channel,private_channel\"}";
    return sendPostRequest("conversations.list", payload);
  }

  /**
   * Consulta conversas (canais, DMs e MPIMs).
   *
   * @param cursor Cursor opcional para paginação (pode ser null)
   * @return JSON da resposta
   * @throws RFWException Em caso de erro
   */
  public String getConversations(String cursor) throws RFWException {
    String payload = cursor == null ? "{\"types\":\"public_channel,private_channel,im,mpim\"}" : "{\"types\":\"public_channel,private_channel,im,mpim\",\"cursor\":\"" + escapeJson(cursor) + "\"}";
    return sendPostRequest("conversations.list", payload);
  }

  /**
   * Consulta completa de usuários pelo nome. Apenas retorna o JSON bruto do Slack (solicitado).
   *
   * @param username Nome exato do usuário (screen_name)
   * @return JSON da lista de usuários
   * @throws RFWException caso a requisição falhe
   */
  public String findUserIdByName(String username) throws RFWException {
    return getUsers();
  }

}
