package br.eng.rodrigogml.rfw.notifier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import br.eng.rodrigogml.rfw.kernel.RFW;
import br.eng.rodrigogml.rfw.kernel.exceptions.RFWException;
import br.eng.rodrigogml.rfw.kernel.preprocess.PreProcess;

/**
 * Integration test for {@link SlackNotifier}. Moved from SlackNotifierTest.
 */
@Tag("integration")
public class SlackNotifierIT {

  private SlackNotifier notifier;
  private String channelTesteId;
  // private final String conversationId;
  private String userId;

  @BeforeEach
  public void setUp() throws RFWException {
    String botToken = RFW.getDevProperty("rfw.notifier.slack.teste.bottoken");
    this.channelTesteId = RFW.getDevProperty("rfw.notifier.slack.teste.channelid");
    // this.conversationId = RFW.getDevProperty("rfw.notifier.slack.teste.conversationid");
    this.userId = RFW.getDevProperty("rfw.notifier.slack.teste.userid");

    PreProcess.requiredNonNullNonEmpty("rfw.notifier.slack.bottoken", botToken);
    PreProcess.requiredNonNullNonEmpty("rfw.notifier.slack.teste.channelid", channelTesteId);
    // PreProcess.requiredNonNullNonEmpty("rfw.notifier.slack.teste.conversationid", conversationId);
    PreProcess.requiredNonNullNonEmpty("rfw.notifier.slack.teste.userid", userId);

    this.notifier = new SlackNotifier(botToken);
  }

  @Test
  public void t00_sendTesteChannelMessage() throws RFWException {
    notifier.sendMessageToChannel(channelTesteId, "Mensagem de teste para o Canal #Teste.");
  }

  // @Test
  // public void t01_sendTesteConversationMessage() throws RFWException {
  // notifier.sendMessageToConversation(conversationId, "Mensagem de teste para conversa configurada.");
  // }

  @Test
  public void t02_sendTesteUserMessage() throws RFWException {
    notifier.sendMessageToUser(userId, "Mensagem de teste para o Usuário configurado.");
  }
}
