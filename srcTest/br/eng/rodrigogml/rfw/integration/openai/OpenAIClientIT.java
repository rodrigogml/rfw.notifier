package br.eng.rodrigogml.rfw.integration.openai;

import static br.eng.rodrigogml.rfw.kernel.RFW.getDevProperty;

public class OpenAIClientIT {

  public static void main(String[] args) throws Exception {
    String secretApiKey = getDevProperty("rfw.openai.apisecret");
    OpenAIClient client = new OpenAIClient(secretApiKey);

    String response = client.sendPrompt("Explique o que é Java em uma frase.");
    System.out.println(response);
  }

}
