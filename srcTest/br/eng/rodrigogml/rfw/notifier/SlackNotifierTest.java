package br.eng.rodrigogml.rfw.notifier;

import org.junit.Test;

import br.eng.rodrigogml.rfw.kernel.RFW;
import br.eng.rodrigogml.rfw.kernel.exceptions.RFWException;
import br.eng.rodrigogml.rfw.kernel.preprocess.PreProcess;

/**
 * Classe de teste para {@link SlackNotifier}. Carrega as configurações necessárias no construtor e envia mensagens para diferentes destinos configurados.
 */
public class SlackNotifierTest {

  private final SlackNotifier notifier;
  private final String channelTesteId;
  // private final String conversationId;
  private final String userId;

  public SlackNotifierTest() throws RFWException {
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
    notifier.sendMessageToChannel(channelTesteId, "Mensagem do Teste Unitário para o Canal #Teste.");
  }

  // @Test
  // public void t01_sendTesteConversationMessage() throws RFWException {
  // notifier.sendMessageToConversation(conversationId, "Mensagem de teste para conversa configurada.");
  // }

  @Test
  public void t02_sendTesteUserMessage() throws RFWException {
    // System.out.println(notifier.getUsers());
    // System.out.println(notifier.getChannels());
    notifier.sendMessageToUser(userId, "Mensagem do Teste Unitário para o Usuário configurado.");
  }
}
