package br.eng.rodrigogml.rfw.notifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.Test;

/**
 * Classe de teste para {@link SlackNotifier}. Carrega as configurações necessárias no construtor e
 * envia mensagens para diferentes destinos configurados.
 */
public class SlackNotifierTest {

  private final SlackNotifier notifier;
  private final String channelId;
  private final String conversationId;
  private final String userId;

  public SlackNotifierTest() throws IOException {
    Properties properties = new Properties();
    try (InputStream input = SlackNotifierTest.class.getResourceAsStream("/slacktest.properties")) {
      if (input == null) {
        throw new IOException("Arquivo slacktest.properties não encontrado nos resources de teste.");
      }
      properties.load(input);
    }

    String botToken = properties.getProperty("SLACK_BOT_TOKEN");
    String defaultChannel = properties.getProperty("SLACK_DEFAULT_CHANNEL");
    this.channelId = properties.getProperty("SLACK_CHANNEL_ID");
    this.conversationId = properties.getProperty("SLACK_CONVERSATION_ID");
    this.userId = properties.getProperty("SLACK_USER_ID");

    validateRequired("SLACK_BOT_TOKEN", botToken);
    validateRequired("SLACK_DEFAULT_CHANNEL", defaultChannel);
    validateRequired("SLACK_CHANNEL_ID", channelId);
    validateRequired("SLACK_CONVERSATION_ID", conversationId);
    validateRequired("SLACK_USER_ID", userId);

    this.notifier = new SlackNotifier(botToken, defaultChannel);
  }

  @Test
  public void enviaParaCanalPadrao() throws IOException {
    notifier.sendMessageToDefaultChannel("Mensagem de integração com Slack enviada pelo canal padrão.");
  }

  @Test
  public void enviaParaCanalEspecifico() throws IOException {
    notifier.sendMessageToChannel(channelId, "Mensagem direcionada para canal especificado.");
  }

  @Test
  public void enviaParaConversa() throws IOException {
    notifier.sendMessageToConversation(conversationId, "Mensagem de teste para conversa configurada.");
  }

  @Test
  public void enviaParaUsuarioDireto() throws IOException {
    notifier.sendMessageToUser(userId, "Mensagem direta para usuário configurado.");
  }

  private void validateRequired(String key, String value) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalStateException(key + " não configurado no slacktest.properties.");
    }
  }
}
