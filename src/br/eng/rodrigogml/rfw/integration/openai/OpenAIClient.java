package br.eng.rodrigogml.rfw.integration.openai;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import br.eng.rodrigogml.rfw.kernel.exceptions.RFWException;
import br.eng.rodrigogml.rfw.kernel.exceptions.RFWWarningException;

/**
 * Cliente Java para comunicação com a OpenAI Chat Completions API.
 *
 * <p>
 * Este client mantém um histórico interno da conversa, incluindo mensagens do usuário, respostas do assistente e instruções de sistema. A OpenAI não mantém estado entre requisições, portanto todo o histórico relevante é reenviado a cada chamada.
 * </p>
 *
 * <p>
 * Os métodos que realizam comunicação HTTP com a OpenAI são sincronizados para garantir consistência do histórico interno. A instância não deve ser compartilhada entre múltiplas threads sem esse cuidado.
 * </p>
 *
 * @author Rodrigo Leitão
 * @since (26 de dez. de 2025)
 */
public class OpenAIClient {

  /**
   * Modelos de chat oficialmente documentados e recomendados pela OpenAI para uso com a API (Chat Completions / Responses).
   *
   * <p>
   * A lista abaixo é baseada na página oficial de modelos da OpenAI: https://platform.openai.com/docs/models
   *
   * Consulte regularmente essa página para atualizações nos nomes dos modelos ou a API de listagem de modelos (`GET /v1/models`) para validar quais estão habilitados para a sua chave. citeturn0search1
   * </p>
   */
  public enum OpenAIModel {

    // GPT-5.2 – modelo de ponta (maior capacidade atual)
    GPT_5_2("gpt-5.2"),

    // GPT-5.1 – modelo avançado da família 5.x recomendado para muitos casos
    GPT_5_1("gpt-5.1"),

    // Variantes menores da família GPT-5 (foco em custo/velocidade) citeturn0search1
    GPT_5_MINI("gpt-5-mini"),
    GPT_5_NANO("gpt-5-nano"),

    // Versões mais antigas ou complementares, ainda úteis em muitas situações citeturn0search1
    GPT_5("gpt-5"),

    // Família GPT-4.1 com variações menores
    GPT_4_1("gpt-4.1"),
    GPT_4_1_MINI("gpt-4.1-mini"),

    // Modelos da família GPT-4o (“omni” – multimodal e poderoso) citeturn0search27
    GPT_4O("gpt-4o"),
    GPT_4O_MINI("gpt-4o-mini");

    private final String modelName;

    OpenAIModel(String modelName) {
      this.modelName = modelName;
    }

    /**
     * Retorna o nome do modelo conforme esperado pela API da OpenAI (parâmetro "model").
     *
     * @return nome do modelo como String
     */
    public String getModelName() {
      return modelName;
    }
  }

  private static final String API_URL = "https://api.openai.com/v1/chat/completions";
  private static final OpenAIModel DEFAULT_MODEL = OpenAIModel.GPT_5_MINI;

  /**
   * Estimativa prática utilizada para cálculo de tokens.
   *
   * <p>
   * A OpenAI não fornece um tokenizer oficial em Java. Como aproximação, considera-se que 1 token equivale, em média, a 4 caracteres em textos em inglês ou português.
   * </p>
   */
  private static final int CHARS_PER_TOKEN = 4;

  private final String apiKey;
  private final String model;

  private final JsonArray history = new JsonArray();
  private JsonObject systemMessage;

  private boolean tokenLimitEnabled = false;
  private int maxTokens;

  /**
   * Cria o client utilizando o modelo padrão.
   *
   * @param apiKey chave de autenticação da OpenAI
   */
  public OpenAIClient(String apiKey) {
    this(apiKey, DEFAULT_MODEL);
  }

  /**
   * Cria o client utilizando um modelo definido via enum.
   *
   * @param apiKey chave de autenticação da OpenAI
   * @param model modelo a ser utilizado nas requisições
   */
  public OpenAIClient(String apiKey, OpenAIModel model) {
    this(apiKey, model.getModelName());
  }

  /**
   * Cria o client utilizando o nome literal do modelo.
   *
   * @param apiKey chave de autenticação da OpenAI
   * @param model nome do modelo conforme esperado pela API
   */
  public OpenAIClient(String apiKey, String model) {
    this.apiKey = apiKey;
    this.model = model;
  }

  /**
   * Envia um prompt simples para a OpenAI sem utilizar ou alterar o histórico interno da conversa.
   *
   * <p>
   * Este método é útil para consultas pontuais, testes ou chamadas stateless. Nenhuma mensagem é armazenada após a execução.
   * </p>
   *
   * @param prompt mensagem enviada ao modelo
   * @return resposta textual do assistente
   * @throws RFWWarningException em caso de erro de comunicação ou erro retornado pela API
   */
  public synchronized String sendPrompt(String prompt) throws RFWException {
    JsonArray messages = new JsonArray();
    messages.add(createMessage("user", prompt));

    JsonObject response = send(messages);
    return extractAssistantMessage(response);
  }

  /**
   * Define ou atualiza as instruções de sistema da conversa.
   *
   * <p>
   * As instruções de sistema controlam o comportamento do modelo (tom, regras, estilo, restrições) e são sempre enviadas como a primeira mensagem da conversa.
   * </p>
   *
   * <p>
   * Caso este método seja chamado novamente, as instruções anteriores são substituídas.
   * </p>
   *
   * @param instructions texto contendo as regras e orientações do assistente
   */
  public void setSystemInstructions(String instructions) {
    this.systemMessage = createMessage("system", instructions);
  }

  /**
   * Envia uma nova mensagem do usuário utilizando o histórico interno.
   *
   * <p>
   * Funcionamento:
   * <ul>
   * <li>A mensagem do usuário é adicionada temporariamente ao histórico</li>
   * <li>O histórico completo (system + mensagens) é enviado à OpenAI</li>
   * <li>Se a resposta for válida, ela é adicionada ao histórico</li>
   * <li>Em caso de erro, a mensagem do usuário é removida</li>
   * </ul>
   * </p>
   *
   * <p>
   * Apenas pares completos (user + assistant) permanecem no histórico, garantindo consistência da conversa.
   * </p>
   *
   * @param message mensagem do usuário
   * @return resposta textual do assistente referente a esta mensagem
   * @throws RFWWarningException em caso de erro retornado pela OpenAI ou falha de comunicação
   */
  public synchronized String sendUserMessage(String message) throws RFWException {
    JsonObject userMessage = createMessage("user", message);
    history.add(userMessage);

    try {
      enforceTokenLimit();
      JsonObject response = send(buildMessagesWithSystem());
      String assistantReply = extractAssistantMessage(response);

      history.add(createMessage("assistant", assistantReply));
      return assistantReply;

    } catch (RuntimeException e) {
      history.remove(history.size() - 1);
      throw e;
    }
  }

  /**
   * Retorna o histórico atual da conversa.
   *
   * <p>
   * O histórico retornado inclui:
   * <ul>
   * <li>Instruções de sistema (se definidas)</li>
   * <li>Todas as mensagens do usuário</li>
   * <li>Todas as respostas do assistente</li>
   * </ul>
   * </p>
   *
   * <p>
   * O objeto retornado é uma representação JSON compatível com a API da OpenAI e pode ser reutilizado, serializado ou persistido.
   * </p>
   *
   * @return JsonArray contendo o histórico completo
   */
  public JsonArray getHistory() {
    return buildMessagesWithSystem();
  }

  /**
   * Ativa a limitação automática de histórico com base em tokens estimados.
   *
   * <p>
   * Antes de cada envio, o histórico é avaliado. Caso o total estimado de tokens ultrapasse o limite configurado, mensagens mais antigas (exceto system) são removidas até que o limite seja respeitado.
   * </p>
   *
   * @param maxTokens número máximo estimado de tokens permitidos
   */
  public void enableTokenLimit(int maxTokens) {
    this.tokenLimitEnabled = true;
    this.maxTokens = maxTokens;
  }

  /**
   * Desativa a limitação automática de tokens.
   *
   * <p>
   * Quando desativado, todo o histórico é enviado independentemente do tamanho.
   * </p>
   */
  public void disableTokenLimit() {
    this.tokenLimitEnabled = false;
  }

  private JsonObject send(JsonArray messages) throws RFWException {
    try {
      JsonObject payload = new JsonObject();
      payload.addProperty("model", model);
      payload.add("messages", messages);

      HttpURLConnection conn = createConnection();
      writePayload(conn, payload.toString());

      int status = conn.getResponseCode();
      String response = readResponse(conn, status);
      JsonObject json = JsonParser.parseString(response).getAsJsonObject();

      if (status >= 400) {
        handleError(json);
      }

      return json;
    } catch (RFWException e) {
      throw e;
    } catch (Exception e) {
      throw new RFWWarningException("Erro ao comunicar com OpenAI: ${0}", new String[] { e.getMessage() }, e);
    }
  }

  private JsonObject createMessage(String role, String content) {
    JsonObject msg = new JsonObject();
    msg.addProperty("role", role);
    msg.addProperty("content", content);
    return msg;
  }

  private JsonArray buildMessagesWithSystem() {
    JsonArray messages = new JsonArray();

    if (systemMessage != null) {
      messages.add(systemMessage);
    }

    for (JsonElement e : history) {
      messages.add(e);
    }

    return messages;
  }

  private String extractAssistantMessage(JsonObject response) {
    return response.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString();
  }

  private void handleError(JsonObject json) throws RFWException {
    JsonObject error = json.getAsJsonObject("error");
    String message = error.has("message") ? error.get("message").getAsString() : "Erro desconhecido";
    String code = error.has("code") ? error.get("code").getAsString() : "unknown";

    throw new RFWWarningException("OpenAI error '${0} - ${1}'", new String[] { code, message });
  }

  private void enforceTokenLimit() {
    if (!tokenLimitEnabled) {
      return;
    }

    while (estimateTokens(buildMessagesWithSystem()) > maxTokens && history.size() > 1) {
      history.remove(0);
    }
  }

  /**
   * Estima o número de tokens do histórico atual.
   *
   * <p>
   * O cálculo é baseado no tamanho do JSON serializado, dividido pela constante média de caracteres por token.
   * </p>
   *
   * <p>
   * Este valor é apenas uma aproximação e pode diferir do valor real utilizado pela OpenAI.
   * </p>
   *
   * @param messages histórico serializado
   * @return número estimado de tokens
   */
  private int estimateTokens(JsonArray messages) {
    int chars = messages.toString().length();
    return chars / CHARS_PER_TOKEN;
  }

  private HttpURLConnection createConnection() throws Exception {
    URL url = new URL(API_URL);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

    conn.setRequestMethod("POST");
    conn.setRequestProperty("Authorization", "Bearer " + apiKey);
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setDoOutput(true);

    return conn;
  }

  private void writePayload(HttpURLConnection conn, String payload) throws Exception {
    try (DataOutputStream out = new DataOutputStream(conn.getOutputStream())) {
      out.write(payload.getBytes("UTF-8"));
    }
  }

  private String readResponse(HttpURLConnection conn, int status) throws Exception {
    BufferedReader reader = new BufferedReader(new InputStreamReader(status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream(), "UTF-8"));

    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      sb.append(line);
    }

    reader.close();
    return sb.toString();
  }
}
