package br.eng.rodrigogml.rfw.integration.openai;

import static br.eng.rodrigogml.rfw.kernel.RFW.getDevProperty;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class OpenAIClientIT {

  @Test
  @Tag("integration")
  public void testSendPrompt() throws Exception {
    String secretApiKey = getDevProperty("rfw.openai.apisecret");
    OpenAIClient client = new OpenAIClient(secretApiKey);

    String response = client.sendPrompt("Explique o que é Java em uma frase.");
    System.out.println(response);
    assertNotNull(response, "Resposta não deve ser nula");
  }

}